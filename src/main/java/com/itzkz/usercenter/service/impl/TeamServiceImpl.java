package com.itzkz.usercenter.service.impl;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itzkz.usercenter.common.ErrorCode;
import com.itzkz.usercenter.enums.TeamStatusEnums;
import com.itzkz.usercenter.exception.BusinessException;
import com.itzkz.usercenter.mapper.TeamMapper;
import com.itzkz.usercenter.mapper.UserMapper;
import com.itzkz.usercenter.model.domain.Team;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.model.domain.UserTeam;
import com.itzkz.usercenter.model.dto.JoinTeamDTO;
import com.itzkz.usercenter.model.dto.TeamQueryDTO;
import com.itzkz.usercenter.model.dto.TeamUpdateDTO;
import com.itzkz.usercenter.model.vo.TeamUserVO;
import com.itzkz.usercenter.model.vo.UserVO;
import com.itzkz.usercenter.service.TeamService;
import com.itzkz.usercenter.service.UserService;
import com.itzkz.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Aaaaaaa
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-04-01 10:50:26
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {
    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;

    /**
     * 增加队伍
     *
     * @param team      队伍对象
     * @param loginUser 登录用户
     * @return
     */
    @Override
    @Transactional
    public long addTeam(Team team, User loginUser) {
        //1. 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final Long userId = loginUser.getId();
        //3. 校验信息

        //   1. 队伍人数 > 1 且 <= 20
        Integer maxnum = Optional.ofNullable(team.getMaxnum()).orElse(0);
        if (maxnum < 1 || maxnum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }

        //   2. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题过长");
        }
        //   3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || (description.length() > 512)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述过长");
        }
        //   4. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnums statusEnums = TeamStatusEnums.getTeamStatusEnumByValue(status);
        if (statusEnums == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (statusEnums.equals(TeamStatusEnums.SECRET)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        //   6. 超时时间 > 当前时间
        if (DateTime.now().isAfter(team.getExpiretime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        //   7. 校验用户最多创建 5 个队伍
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Team::getUserid, userId);
        long count = this.count(queryWrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超过最大限制创建队伍数");
        }

        //4. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserid(userId);
        boolean result = this.save(team);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        //5. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamid(team.getId());
        userTeam.setUserid(userId);
        userTeam.setJointime(DateTime.now());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        return team.getId();
    }

    /**
     * 更新队伍
     *
     * @param teamUpdateDTO 更新队伍参数对象
     * @param loginUser     登录用户
     * @return
     */
    @Override
    @Transactional
    public boolean updateTeam(TeamUpdateDTO teamUpdateDTO, User loginUser) {
        if (teamUpdateDTO == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = teamUpdateDTO.getId();
        // 获取待更新的团队信息
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查团队信息是否发生变化，如果没有变化直接返回true
        if (isUnchanged(oldTeam, teamUpdateDTO)) {
            return true;
        }

        // 检查用户权限
        boolean admin = userService.isAdmin(loginUser);
        if (!admin && !Objects.equals(loginUser.getId(), oldTeam.getUserid())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        // 处理团队状态
        TeamStatusEnums statusEnums = TeamStatusEnums.getTeamStatusEnumByValue(teamUpdateDTO.getStatus());
        if (statusEnums == null) {
            statusEnums = TeamStatusEnums.PUBLIC;
        }

        // 如果团队状态为私有，则必须设置密码
        if (TeamStatusEnums.PRIVATE.equals(statusEnums)) {
            if (StringUtils.isBlank(teamUpdateDTO.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "请设置房间密码");
            }
        }
        UpdateWrapper<Team> updateWrapper = new UpdateWrapper<>();
        // 如果团队状态从私有变为公有，则清空密码字段
        if (statusEnums == TeamStatusEnums.PUBLIC) {
            if (StringUtils.isNotBlank(teamUpdateDTO.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间公开状态不允许设置密码");
            }
            String password = oldTeam.getPassword();
            if (StringUtils.isNotBlank(password)) {
                updateWrapper.set("password", "");
            }
        }
        // 将团队更新DTO转换为团队对象，并执行更新操作
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateDTO, team);
        boolean result = this.updateById(team);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return true;
    }

    /**
     * 用户加入队伍
     *
     * @param joinTeamDTO 加入队伍参数对象
     * @param loginUser   登录用户
     * @return
     */
    @Override
    @Transactional
    public boolean joinTeam(JoinTeamDTO joinTeamDTO, User loginUser) {
        if (joinTeamDTO == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断用户不能超过加入队伍上限
        Long userId = loginUser.getId();
        LambdaQueryWrapper<UserTeam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTeam::getUserid, userId);
        long userJoinTeamNum = userTeamService.count(queryWrapper);
        if (userJoinTeamNum < 0 || userJoinTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已超过最大限制房间数");
        }
        //判断房间是否存在
        long id = joinTeamDTO.getTeamid();
        Team team = this.getById(id);
        //如果不存在
        if (team == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间不存在");
        }
        //如果存在只能加入未过期的队伍
        Date expiretime = team.getExpiretime();
        if (!(expiretime == null) && expiretime.before(DateTime.now())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期，禁止加入");
        }

        //如果存在只能加入未满人数的队伍

        Long teamId = team.getId();
        queryWrapper.eq(UserTeam::getTeamid, teamId);
        long teamHasUserNum = userTeamService.count(queryWrapper);
        if (teamHasUserNum < 0 || teamHasUserNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
        }

        //禁止加入私人队伍

        Integer status = team.getStatus();
        TeamStatusEnums statusEnums = TeamStatusEnums.getTeamStatusEnumByValue(status);
        if (statusEnums == TeamStatusEnums.PRIVATE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私密队伍");
        }

        //加入加密房间需要密码正确
        if (statusEnums == TeamStatusEnums.SECRET) {
            if (StringUtils.isBlank(joinTeamDTO.getPassword()) || !joinTeamDTO.getPassword().equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入密码有误,请重试");
            }
        }


        //禁止加入已加入的队伍
        queryWrapper.eq(UserTeam::getTeamid, teamId).
                eq(UserTeam::getUserid, userId);
        UserTeam hasUserJoinTeam = userTeamService.getOne(queryWrapper);
        if (hasUserJoinTeam != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入队伍");
        }

        //新增队伍 - 用户关联信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserid(userId);
        userTeam.setTeamid(teamId);
        userTeam.setJointime(new Date());
        boolean result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }


        return true;
    }

    /**
     * 退出队伍
     *
     * @param teamId    队伍id
     * @param loginUser 登录用户
     * @return
     */
    @Override
    @Transactional
    public boolean quitTeam(Long teamId, User loginUser) {
        if (teamId <= 0 || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间不存在");
        }

        // 查询房间的人数
        LambdaQueryWrapper<UserTeam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTeam::getTeamid, teamId);
        long teamUserCount = userTeamService.count(queryWrapper);

        if (teamUserCount == 1) {
            // 如果房间只有一个人，直接移除
            return this.removeById(teamId);
        } else {
            if (Objects.equals(team.getUserid(), loginUser.getId())) {
                // 如果当前用户是队长，找到下一个队长并转让队长职责
                LambdaQueryWrapper<UserTeam> teamLambdaQueryWrapper = new LambdaQueryWrapper<>();
                teamLambdaQueryWrapper.eq(UserTeam::getTeamid, teamId);
                //下一个队长对象
                UserTeam nextLeader = userTeamService.getOne(queryWrapper.orderByAsc(UserTeam::getId).last("LIMIT 1 OFFSET 1"));
                //转移队长
                if (nextLeader != null) {
                    team.setUserid(nextLeader.getUserid());
                    this.updateById(team);
                } else {
                    // 没有找到下一个队长，则可能是出现了异常情况
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到下一个队长");
                }
            }

            // 移除队伍用户关系
            queryWrapper.clear();
            queryWrapper.eq(UserTeam::getUserid, loginUser.getId()).eq(UserTeam::getTeamid, teamId);
            return userTeamService.remove(queryWrapper);
        }
    }

    /**
     * 解散队伍
     *
     * @param id        队伍id
     * @param loginUser 登录用户
     * @return
     */
    @Override
    @Transactional
    public boolean deleteTeam(long id, User loginUser) {
        // 校验请求参数
        if (id <= 0 || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //校验队伍是否存在
        Team team = this.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        //校验你是不是队伍的队长
        if (!team.getUserid().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "不是队长 不允许解散队伍");
        }
        //删除队伍
        boolean result = this.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解散房间失败");
        }
        //移除所有加入队伍的关联信息
        LambdaQueryWrapper<UserTeam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTeam::getTeamid, id);
        result = userTeamService.remove(queryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍用户信息失败");
        }
        return true;
    }

    /**
     * 查询自己创建的队伍列表信息
     *
     * @param loginUser 登录用户
     * @return 封装队伍信息对象
     */
    @Override
    public List<TeamUserVO> myCreateTeamsList(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<Team> teamList = lambdaQuery().eq(Team::getUserid, loginUser.getId()).list();
        return getTeamUserVOS(teamList);
    }

    /**
     * 查询自己加入的所有队伍列表信息
     *
     * @param loginUser 登录用户
     * @return 封装队伍信息对象
     */
    @Override
    public List<TeamUserVO> myJoinTeamsList(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<UserTeam> queryWrapper = new LambdaQueryWrapper<>();
        List<Long> teamIdList = userTeamService.list(queryWrapper.eq(UserTeam::getUserid, loginUser.getId())).stream().map(UserTeam::getTeamid).collect(Collectors.toList());
        List<Team> teamList = lambdaQuery().in(Team::getId, teamIdList).list();
        return getTeamUserVOS(teamList);

    }

    /**
     * 查询队伍信息
     *
     * @param teamQuery 查询队伍参数对象
     * @param isAdmin   是否为管理员
     * @return 封装队伍信息对象
     */
    @Override
    public List<TeamUserVO> listTeam(TeamQueryDTO teamQuery, Boolean isAdmin) {
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        if (teamQuery != null) {
            //根据队伍id查询
            Long id = teamQuery.getId();
            if (id != null) {
                queryWrapper.eq(Team::getId, id);
            }
            //根据队伍名称查询
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like(Team::getName, name);
            }
            //根据队伍描述查询
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like(Team::getDescription, description);
            }
            //根据队伍名称和描述查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)){
                queryWrapper.and(q->q.like(Team::getName, searchText).or().like(Team::getDescription, searchText));
            }
            //根据队伍最大人数查询
            Integer maxNum = teamQuery.getMaxnum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq(Team::getMaxnum, maxNum);
            }
            //根据用户（队长）id查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq(Team::getUserid, userId);
            }
//            //根据队伍状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnums statusEnums = TeamStatusEnums.getTeamStatusEnumByValue(status);
            if (statusEnums == null) {
                queryWrapper.and(q -> q.eq(Team::getStatus, TeamStatusEnums.PUBLIC.getValue()).or().eq(Team::getStatus, TeamStatusEnums.SECRET.getValue()));
            }
            if (!isAdmin && statusEnums == TeamStatusEnums.PRIVATE) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }

            //根据过期时间查询
            Date expiretime = teamQuery.getExpiretime();
            if (expiretime != null && expiretime.after(DateTime.now())) {
                queryWrapper.eq(Team::getExpiretime, expiretime);
            }
        }
        //不展示已过期的队伍
        //expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt(Team::getExpiretime, DateTime.now()).or().isNull(Team::getExpiretime));

        // 查询队伍和已加入队伍成员的信息
        List<Team> teamList = this.list(queryWrapper);
        return getTeamUserVOS(teamList);
    }

    /**
     * 在队伍中的用户
     *
     * @param teamList 队伍列表
     * @return
     */
    private ArrayList<TeamUserVO> getTeamUserVOS(List<Team> teamList) {
        if (teamList == null) {
            return new ArrayList<>();
        }
        ArrayList<TeamUserVO> teamUserVOArrayList = new ArrayList<>();
        LambdaQueryWrapper<UserTeam> userTeamQueryWrapper = new LambdaQueryWrapper<>();

        for (Team team : teamList) {
            Long teamId = team.getId();
            if (teamId == null) {
                continue;
            }
            userTeamQueryWrapper.clear(); // 清空查询条件
            userTeamQueryWrapper.eq(UserTeam::getTeamid, teamId);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            if (userTeamList == null) {
                continue;
            }
            HashSet<UserVO> joinTeamUser = new HashSet<>(); // 创建新的用户集合
            TeamUserVO teamUserVO = new TeamUserVO();
            for (UserTeam userTeam : userTeamList) {
                Long userid = userTeam.getUserid();
                User user = userMapper.selectById(userid);
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                joinTeamUser.add(userVO); // 将用户添加到集合中
            }
            teamUserVO.setJoinTeamUser(joinTeamUser);
            BeanUtils.copyProperties(team, teamUserVO);
            teamUserVOArrayList.add(teamUserVO);
        }
        return teamUserVOArrayList;
    }


//    @Override
//    public List<TeamUserVO> listTeam(TeamQueryDTO teamQuery, Boolean isAdmin) {
//
//        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
////        if (teamQuery != null) {
////            // 根据查询条件设置查询条件
////            // ...
////        }
//        // 不展示已过期的队伍
//        queryWrapper.and(qw -> qw.gt(Team::getExpiretime, DateTime.now()).or().isNull(Team::getExpiretime));
//
//        // 查询队伍和已加入队伍成员的信息
//        queryWrapper.select(Team::getId, Team::getName, Team::getDescription, Team::getUserid);
//        List<Team> teamList = this.list(queryWrapper);
//        // 构建队伍和成员信息的映射关系
//        Map<Long, TeamUserVO> teamUserMap = new HashMap<>();
//        for (Team team : teamList) {
//            if (team != null) { // 添加空指针检查
//                TeamUserVO teamUserVO = new TeamUserVO();
//                BeanUtils.copyProperties(team, teamUserVO);
//                teamUserMap.put(team.getId(), teamUserVO);
//            }
//        }
//        // 查询队伍成员信息
//        if (!teamList.isEmpty()) {
//            List<Long> teamIds = teamList.stream().map(Team::getId).collect(Collectors.toList());
//            LambdaQueryWrapper<UserTeam> userTeamQueryWrapper = new LambdaQueryWrapper<>();
//            userTeamQueryWrapper.in(UserTeam::getTeamid, teamIds);
//            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
//
//            // 将用户信息添加到对应的队伍信息中
//            for (UserTeam userTeam : userTeamList) {
//                if (userTeam != null) { // 添加空指针检查
//                    Long teamId = userTeam.getTeamid();
//                    TeamUserVO teamUserVO = teamUserMap.get(teamId);
//                    if (teamUserVO != null) {
//                        User user = userMapper.selectById(userTeam.getUserid());
//                        if (user != null) { // 添加空指针检查
//                            UserVO userVO = new UserVO();
//                            BeanUtils.copyProperties(user, userVO);
////                            teamUserVO.getJoinTeamUser().add(userVO);
//
//                        }
//                    }
//                }
//            }
//        }
//        return new ArrayList<>(teamUserMap.values());


    /**
     * 判断原值与修改值是否相同 以减少对数据库的操作
     *
     * @param team
     * @param teamUpdateDTO
     * @return
     */
    private boolean isUnchanged(Team team, TeamUpdateDTO teamUpdateDTO) {
        if (team == null || teamUpdateDTO == null) {
            return false; // 如果任一对象为null，则认为发生了变化
        }
        // 比较所有字段是否相等
        boolean idUnchanged = teamUpdateDTO.getId() == team.getId();
        boolean nameUnchanged = Objects.equals(teamUpdateDTO.getName(), team.getName());
        boolean descriptionUnchanged = Objects.equals(teamUpdateDTO.getDescription(), team.getDescription());
        boolean maxnumUnchanged = Objects.equals(teamUpdateDTO.getMaxnum(), team.getMaxnum());
        boolean expiretimeUnchanged = Objects.equals(teamUpdateDTO.getExpiretime(), team.getExpiretime());
        boolean statusUnchanged = Objects.equals(teamUpdateDTO.getStatus(), team.getStatus());
        boolean passwordUnchanged = Objects.equals(teamUpdateDTO.getPassword(), team.getPassword());
        // 如果所有字段都相等，则返回true，否则返回false
        return idUnchanged && nameUnchanged && descriptionUnchanged &&
                maxnumUnchanged && expiretimeUnchanged && statusUnchanged && passwordUnchanged;
    }
}






