package com.shanyangcode.zhixing_travel_assistant_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.zhixing_travel_assistant_backend.model.Message;

public interface MessageService extends IService<Message> {
    Page<Message> pageByConversation(String userId, String conversationId, long page, long size);
}
