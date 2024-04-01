package com.itzkz.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itzkz.usercenter.common.BaseResponse;
import com.itzkz.usercenter.common.ErrorCode;
import com.itzkz.usercenter.common.ResultUtils;
import com.itzkz.usercenter.exception.BusinessException;
import com.itzkz.usercenter.model.domain.Team;
import com.itzkz.usercenter.model.dto.TeamQueryDTO;
import com.itzkz.usercenter.model.dto.TeamUpdateDTO;
import com.itzkz.usercenter.service.TeamService;
import io.swagger.annotations.Api;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Api(tags = "队伍接口")
@RequestMapping("/team")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TeamController {

    @Resource
    private TeamService teamService;

    /**
     * 添加组队
     *
     * @param team 队伍实体类
     * @return 统一响应类
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody Team team) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = teamService.save(team);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(team.getId());
    }

    /**
     * 删除队伍
     *
     * @param id 队伍id
     * @return 统一响应类
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.removeById(id);

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
    public BaseResponse<Boolean> addTeam(@RequestBody TeamUpdateDTO teamUpdateDTO) {
        if (teamUpdateDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateDTO, team);
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        boolean result = teamService.update(team, queryWrapper);
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
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
    public BaseResponse<List<Team>> listTeam(@RequestBody TeamQueryDTO teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>(team);
        List<Team> teamList = teamService.list(queryWrapper);

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

}
