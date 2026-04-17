package com.zyt.consultant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillOption {

    private String id;
    private String name;
    private String description;
    private String promptInstruction;
}

