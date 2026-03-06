package com.shanyangcode.zhixing_travel_assistant_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement.RequirementAgent;
import com.shanyangcode.zhixing_travel_assistant_backend.mapper.ConversationMapper;
import com.shanyangcode.zhixing_travel_assistant_backend.mapper.StoreMapper;
import com.shanyangcode.zhixing_travel_assistant_backend.model.Conversation;
import com.shanyangcode.zhixing_travel_assistant_backend.model.Message;
import com.shanyangcode.zhixing_travel_assistant_backend.service.ChatService;
import com.shanyangcode.zhixing_travel_assistant_backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private RequirementAgent requirementAgent;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Flux<String> chat(String userId, String sessionId, String prompt) {
        return Mono.fromCallable(() -> {
                    ensureConversationExists(userId, sessionId);
                    insertMessage(sessionId, (short) 1, prompt);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(requirementAgent.executeState(sessionId, prompt))
                .flatMap(state -> Mono.fromCallable(() -> {
                            String output = state.output();
                            insertMessage(sessionId, (short) 2, output);
                            updateConversationUpdatedAt(sessionId);

                            java.util.Map<String, Object> value = new java.util.HashMap<>();
                            value.put("requirements", state.requirements());
                            value.put("output", output);
                            storeMapper.upsert(new String[]{"conversations", sessionId}, "latest_plan", objectMapper.writeValueAsString(value));
                            return output;
                        }).subscribeOn(Schedulers.boundedElastic())
                )
                .onErrorResume(e -> Mono.just("系统繁忙，请稍后再试"))
                .flatMapMany(Flux::just);
    }

    private void ensureConversationExists(String userId, String conversationId) {
        Conversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getIsDelete, 0)
                .last("limit 1"));

        if (existing != null) {
            return;
        }

        Conversation conversation = Conversation.builder().build();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        conversation.setTitle("新的会话");
        conversation.setCreatedAt(java.time.OffsetDateTime.now());
        conversation.setUpdatedAt(java.time.OffsetDateTime.now());
        conversation.setIsDelete((short) 0);
        conversationMapper.insert(conversation);
    }

    private void updateConversationUpdatedAt(String conversationId) {
        Conversation conversation = Conversation.builder().build();
        conversation.setId(conversationId);
        conversation.setUpdatedAt(java.time.OffsetDateTime.now());
        conversationMapper.updateById(conversation);
    }

    private void insertMessage(String conversationId, short role, String content) {
        Message message = Message.builder().build();
        message.setId(java.util.UUID.randomUUID().toString());
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setIsDelete((short) 0);
        messageService.save(message);
    }
}