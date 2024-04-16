package com.itzkz.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itzkz.usercenter.common.BaseResponse;
import com.itzkz.usercenter.common.ErrorCode;
import com.itzkz.usercenter.common.ResultUtils;
import com.itzkz.usercenter.exception.BusinessException;
import com.itzkz.usercenter.model.domain.Team;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.model.dto.*;
import com.itzkz.usercenter.model.vo.TeamUserVO;
import com.itzkz.usercenter.service.TeamService;
import com.itzkz.usercenter.service.UserService;
import io.swagger.annotations.Api;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Api(tags = "队伍接口")
@RequestMapping("/team")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TeamController {

    @Resource
    private TeamService teamService;
    @Resource
    private UserService userService;

    /**
     * 添加组队
     *
     * @param team 队伍实体类
     * @return 统一响应类
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody Team team, HttpServletRequest request) {
        if (team == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long result = teamService.addTeam(team, loginUser);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(result);
    }

    /**
     * 解散队伍
     *
     * @param deleteTeamDTO 解散队伍对象
     * @return 统一响应类
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteTeamDTO deleteTeamDTO, HttpServletRequest request) {
        if (deleteTeamDTO==null||deleteTeamDTO.getTeamid() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(deleteTeamDTO.getTeamid(), loginUser);

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(true);
    }
    /**
     * 更新队伍
     *
     * @param teamUpdateDTO 包含待更新队伍信息的数据传输对象
     * @return 统一响应类
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateDTO teamUpdateDTO, HttpServletRequest request) {
        if (teamUpdateDTO == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.updateTeam(teamUpdateDTO, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取查询队伍
     *
     * @param id 队伍id
     * @return 统一响应类
     */
    @GetMapping("/get/{id}")
    public BaseResponse<Team> getTeamById(@PathVariable long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未查到该用户");
        }
        return ResultUtils.success(team);
    }

    /**
     * 查询所有队伍
     *
     * @param teamQuery 查询条件对象，用于指定查询参数
     * @return 统一响应类
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQueryDTO teamQuery, HttpServletRequest request) {
        if (teamQuery == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        User loginUser = userService.getLoginUser(request);
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery, isAdmin,loginUser);

        return ResultUtils.success(teamList);
    }
    /**
     * 查询自己创建的队伍信息
     *
     * @param request 请求
     * @return 统一响应类
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> myCreateTeamsList(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        List<TeamUserVO> teamList = teamService.myCreateTeamsList(loginUser);
        return ResultUtils.success(teamList);
    }

    /**
     * 查询自己加入的所有队伍信息
     *
     * @param request 请求
     * @return 统一响应类
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> myJoinTeamsList(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        List<TeamUserVO> teamList = teamService.myJoinTeamsList(loginUser);
        return ResultUtils.success(teamList);
    }

    /**
     * 分页查询队伍列表
     *
     * @param teamQuery 查询条件对象，用于指定查询参数
     * @return 统一响应类
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamByPage(@RequestBody TeamQueryDTO teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> teamPage = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(teamPage, queryWrapper);

        return ResultUtils.success(resultPage);
    }

    /**
     * 用户加入队伍
     *
     * @param joinTeamDTO 加入条件对象,用户指定加入队伍参数
     * @param request     请求
     * @return 统一响应类
     */

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody JoinTeamDTO joinTeamDTO, HttpServletRequest request) {
        if (joinTeamDTO == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(teamService.joinTeam(joinTeamDTO, loginUser));

    }


    /**
     * 用户退出队伍
     *
     * @param quitTeamDTO  退出队伍参数对象
     * @param request 请求
     * @return 统一响应类
     */

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody QuitTeamDTO quitTeamDTO, HttpServletRequest request) {
        if (quitTeamDTO==null||quitTeamDTO.getTeamid() <= 0 || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(teamService.quitTeam(quitTeamDTO.getTeamid(), loginUser));

    }




}
