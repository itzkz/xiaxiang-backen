package com.itzkz.usercenter.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itzkz.usercenter.model.domain.Team;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.model.dto.JoinTeamDTO;
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
    /**
     * 增加队伍
     * @param team 队伍对象
     * @param loginUser 登录用户
     * @return 队伍id
     */
    long addTeam(Team team , User loginUser);

    /**
     * 查询队伍
     * @param teamQuery 查询队伍参数对象
     * @param isAdmin 是否为管理员
     * @return 返回封装对象列表
     */
    List<TeamUserVO> listTeam(TeamQueryDTO teamQuery, Boolean isAdmin);

    /**
     * 更新队伍信息
     * @param teamUpdateDTO 更新队伍参数对象
     * @param loginUser 登录用户
     * @return Boolean
     */
    boolean updateTeam(TeamUpdateDTO teamUpdateDTO,User loginUser);

    /**
     * 用户加入队伍
     * @param joinTeamDTO 加入队伍参数对象
     * @param loginUser 登录用户
     * @return boolean
     */
    boolean joinTeam(JoinTeamDTO joinTeamDTO, User loginUser);

    /**
     * 用户退出队伍
     * @param teamId 队伍id
     * @param loginUser 登录用户
     * @return boolean
     */
    boolean quitTeam(Long teamId, User loginUser);

    /**
     * 解散队伍
     * @param id 队伍id
     * @return boolean
     */
    boolean dismissTeam(long id,User loginUser);
}
