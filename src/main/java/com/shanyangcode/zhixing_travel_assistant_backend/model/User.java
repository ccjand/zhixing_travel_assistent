package com.shanyangcode.zhixing_travel_assistant_backend.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
@TableName("\"user\"") //user在postgresql是关键字，得用双引号括起来
public class User {

    @TableId("id")
    private String id;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("preferences")
    private String preferences;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("is_delete")
    private Short isDelete;
}
