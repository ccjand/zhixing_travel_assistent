package com.shanyangcode.zhixing_travel_assistant_backend.service;

import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<String> chat(String userId, String sessionId, String prompt);
}
