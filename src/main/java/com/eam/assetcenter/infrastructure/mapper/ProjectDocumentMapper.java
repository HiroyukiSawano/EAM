package com.eam.assetcenter.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eam.assetcenter.domain.entity.ProjectDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目文档 Mapper。
 */
@Mapper
public interface ProjectDocumentMapper extends BaseMapper<ProjectDocument> {
}
