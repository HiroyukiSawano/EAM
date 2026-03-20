package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.Department;
import com.eam.assetcenter.infrastructure.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门业务服务，负责部门主数据维护。
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增资源记录。
     */
    public Department create(Department department) {
        supportService.ensureUniqueDepartmentCode(department.getCode(), null);
        departmentMapper.insert(department);
        auditService.record("DEPARTMENT", department.getId(), AuditActionType.CREATE, "Created department " + department.getCode(), "SYSTEM");
        return department;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    public Department update(Long id, Department department) {
        Department existing = getById(id);
        supportService.ensureUniqueDepartmentCode(department.getCode(), id);
        department.setId(id);
        departmentMapper.updateById(department);
        auditService.record("DEPARTMENT", id, AuditActionType.UPDATE, "Updated department " + existing.getCode(), "SYSTEM");
        return getById(id);
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
     * 删除指定主键对应的资源记录。
     */
    public void delete(Long id) {
        getById(id);
        departmentMapper.deleteById(id);
        auditService.record("DEPARTMENT", id, AuditActionType.DELETE, "Deleted department " + id, "SYSTEM");
    }
}





