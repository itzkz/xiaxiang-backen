package com.itzkz.usercenter.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itzkz.usercenter.mapper.TeamMapper;
import com.itzkz.usercenter.model.domain.Team;
import com.itzkz.usercenter.service.TeamService;
import org.springframework.stereotype.Service;

/**
 * @author Aaaaaaa
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-04-01 10:50:26
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

}




