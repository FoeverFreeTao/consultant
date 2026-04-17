package com.zyt.consultant.service;

import com.zyt.consultant.entity.SkillOption;

import java.util.List;

public interface SkillService {

    List<SkillOption> listAllSkills();

    List<SkillOption> getUserSkills(Long userId);

    boolean applyUserSkills(Long userId, List<String> skillIds);

    String buildSkillPromptByMemoryId(String memoryId);

    String buildSkillPromptBySkillIds(List<String> skillIds);
}

