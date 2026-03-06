package com.shanyangcode.zhixing_travel_assistant_backend.service;

import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.ConversationListResp;

import java.util.List;

public interface ConversationService {

    List<ConversationListResp> listByUid(String userId);

    ConversationListResp create(String userId);
}
