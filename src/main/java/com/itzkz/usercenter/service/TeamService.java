package com.itzkz.usercenter.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itzkz.usercenter.model.domain.Team;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.model.dto.TeamQueryDTO;
import com.itzkz.usercenter.model.dto.TeamUpdateDTO;
import com.itzkz.usercenter.model.vo.TeamUserVO;

import java.util.List;

/**
* @author Aaaaaaa
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-04-01 10:50:26
*/
public interface TeamService extends IService<Team> {

    long addTeam(Team team , User loginUser);


    List<TeamUserVO> listTeam(TeamQueryDTO teamQuery, Boolean isAdmin);


    boolean updateTeam(TeamUpdateDTO teamUpdateDTO,User loginUser);
}
