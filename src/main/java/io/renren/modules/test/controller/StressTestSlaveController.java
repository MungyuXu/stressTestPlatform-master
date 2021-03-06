package io.renren.modules.test.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestSlaveEntity;
import io.renren.modules.test.jmeter.JmeterRunEntity;
import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.service.StressTestSlaveService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分布式节点管理
 */
@RestController
@RequestMapping("/test/stressSlave")
public class StressTestSlaveController {
    @Autowired
    private StressTestSlaveService stressTestSlaveService;

    @Autowired
    private StressTestFileService stressTestFileService;

    /**
     * 分布式节点列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("test:stress:slaveList")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(StressTestUtils.filterParms(params));
        List<StressTestSlaveEntity> stressTestList = stressTestSlaveService.queryList(query);
        int total = stressTestSlaveService.queryTotal(query);

        PageUtils pageUtil = new PageUtils(stressTestList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 性能测试分布式节点信息
     */
    @RequestMapping("/info/{slaveId}")
    @RequiresPermissions("test:stress:slaveInfo")
    public R info(@PathVariable("slaveId") Long slaveId) {
        StressTestSlaveEntity stressTestSlave = stressTestSlaveService.queryObject(slaveId);

        return R.ok().put("stressTestSlave", stressTestSlave);
    }

    /**
     * 保存性能测试分布式节点
     */
    @SysLog("保存性能测试分布式节点信息")
    @RequestMapping("/save")
    @RequiresPermissions("test:stress:slaveSave")
    public R save(@RequestBody StressTestSlaveEntity stressTestSlave) {
        ValidatorUtils.validateEntity(stressTestSlave);

        stressTestSlaveService.save(stressTestSlave);

        return R.ok();
    }

    /**
     * 修改性能测试分布式节点信息
     */
    @SysLog("修改性能测试分布式节点信息")
    @RequestMapping("/update")
    @RequiresPermissions("test:stress:slaveUpdate")
    public R update(@RequestBody StressTestSlaveEntity stressTestSlave) {
        ValidatorUtils.validateEntity(stressTestSlave);

        stressTestSlaveService.update(stressTestSlave);

        return R.ok();
    }

    /**
     * 删除性能测试分布式节点
     */
    @SysLog("删除性能测试分布式节点")
    @RequestMapping("/delete")
    @RequiresPermissions("test:stress:slaveDelete")
    public R delete(@RequestBody Long[] slaveIds) {
        stressTestSlaveService.deleteBatch(slaveIds);

        return R.ok();
    }


    /**
     * 切换性能测试分布式节点状态
     */
    @SysLog("切换性能测试分布式节点状态")
    @RequestMapping("/batchUpdateStatus")
    @RequiresPermissions("test:stress:slaveStatusUpdate")
    public R batchUpdateStatus(@RequestParam(value = "slaveIds[]") List<Long> slaveIds,
                               @RequestParam(value = "status") Integer status) {
        for (Long slaveId : slaveIds) {
            stressTestSlaveService.updateBatchStatus(slaveId, status);
        }

        return R.ok();
    }

    /**
     * 强制切换性能测试分布式节点状态
     */
    @SysLog("强制切换性能测试分布式节点状态")
    @RequestMapping("/batchUpdateStatusForce")
    @RequiresPermissions("test:stress:slaveStatusUpdateForce")
    public R batchUpdateStatusForce(@RequestParam(value = "slaveIds[]") List<Long> slaveIds,
                                    @RequestParam(value = "status") Integer status) {
        stressTestSlaveService.updateBatchStatusForce(slaveIds, status);
        return R.ok();
    }

    /**
     * 重启已经启动的性能测试分布式节点（停止状态的分布式节点不变）
     */
    @SysLog("重启已经启动的性能测试分布式节点")
    @RequestMapping("/batchRestart")
    @RequiresPermissions("test:stress:slaveRestart")
    public R batchRestart(@RequestParam(value = "slaveIds[]") List<Long> slaveIds) {

        for (Long slaveId : slaveIds) {
            stressTestSlaveService.restartSingle(slaveId);
        }
        return R.ok();
    }

    /**
     * 复制分布式节点信息
     */
    @SysLog("复制分布式节点")
    @RequestMapping("/copySlave")
    @RequiresPermissions("test:stress:copySlave")
    public R copySlave(@RequestBody Long[] slaveIds) {
        for (Long slaveId : slaveIds) {
            StressTestSlaveEntity stressTestSlave = stressTestSlaveService.queryObject(slaveId);
            StressTestSlaveEntity copyEntity = stressTestSlave.copySlaveEntity();
            stressTestSlaveService.save(copyEntity);
        }
        return R.ok();
    }

    /**
     *查询当前并发线程总数
     */
    @SysLog("查询并发线程总数")
    @RequestMapping("/totalThreads")
    public R totalThreads() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        List<StressTestFileEntity> jobList = stressTestFileService.queryList(map);
        int totalThread = 0;
        for (StressTestFileEntity stressTestFileEntity: jobList) {
            Long fileId = stressTestFileEntity.getFileId();
            totalThread = totalThread + (int) StressTestUtils.jMeterEntity4file.get(fileId).getNumberOfActiveThreads().get("Active");
        }

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("status", 1);
        int totalAvailableSlaves = stressTestSlaveService.queryTotal(queryMap);
        R r = new R();
        r.put("totalThread", totalAvailableSlaves * 1000);
        r.put("availableThread", totalAvailableSlaves * 1000 - totalThread);
        return R.ok().put("threadInfo", r);
    }
}
