package com.shanyangcode.zhixing_travel_assistant_backend.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("conversation")
@Builder
public class Conversation {

    @TableId("id")
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("title")
    private String title;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("is_delete")
    private Short isDelete;
}
