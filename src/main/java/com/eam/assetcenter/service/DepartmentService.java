package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.domain.entity.Department;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.DepartmentMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门业务服务，负责部门主数据维护。
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final PersonMapper personMapper;
    private final AssetHardwareMapper assetHardwareMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Department create(Department department) {
        supportService.ensureUniqueDepartmentCode(department.getCode(), null);
        supportService.ensureCommonStatusValid(department.getStatus(), "部门");
        departmentMapper.insert(department);
        auditService.record("DEPARTMENT", department.getId(), AuditActionType.CREATE, "Created department " + department.getCode(), "SYSTEM");
        return department;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Department update(Long id, Department department) {
        Department existing = getById(id);
        supportService.ensureUniqueDepartmentCode(department.getCode(), id);
        supportService.ensureCommonStatusValid(department.getStatus(), "部门");
        updateDepartment(id, department);
        auditService.record("DEPARTMENT", id, AuditActionType.UPDATE, "Updated department " + existing.getCode(), "SYSTEM");
        return getById(id);
    }

    private void updateDepartment(Long id, Department department) {
        departmentMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Department>()
                        .eq(Department::getId, id)
                        .set(Department::getParentId, department.getParentId())
                        .set(Department::getCode, department.getCode())
                        .set(Department::getName, department.getName())
                        .set(Department::getStatus, department.getStatus())
                        .set(Department::getUpdatedAt, LocalDateTime.now()));
    }

    /**
     * 根据主键查询资源记录，不存在时抛出业务异常。
     */
    public Department getById(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException("Department not found: " + id);
        }
        return department;
    }

    /**
     * 按条件分页查询资源列表。
     */
    public PageResponse<Department> page(int pageNo, int pageSize, String keyword) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<Department>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(Department::getName, keyword).or().like(Department::getCode, keyword))
                .orderByAsc(Department::getCode);
        return PageResponse.from(departmentMapper.selectPage(new Page<Department>(pageNo, pageSize), wrapper));
    }

    /**
     * 查询全部资源列表。
     */
    public List<Department> listAll() {
        return departmentMapper.selectList(new LambdaQueryWrapper<Department>().orderByAsc(Department::getCode));
    }

    /**
     * 删除指定主键对应的资源记录，删除前检查是否被人员或硬件资产引用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        // 检查是否被人员引用
        Long personCount = personMapper.selectCount(new LambdaQueryWrapper<Person>().eq(Person::getDepartmentId, id));
        if (personCount > 0) {
            throw new BusinessException("该部门下仍有 " + personCount + " 名人员，无法删除");
        }
        // 检查是否被硬件资产引用
        Long hardwareCount = assetHardwareMapper.selectCount(new LambdaQueryWrapper<AssetHardware>().eq(AssetHardware::getDepartmentId, id));
        if (hardwareCount > 0) {
            throw new BusinessException("该部门下仍有 " + hardwareCount + " 件硬件资产，无法删除");
        }
        departmentMapper.deleteById(id);
        auditService.record("DEPARTMENT", id, AuditActionType.DELETE, "Deleted department " + id, "SYSTEM");
    }
}





