package com.sky.service;

import com.github.pagehelper.Page;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.EmployeePasswordDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {
    //员工登录//
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    //加入新员工//
    void save(EmployeeDTO employeeDTO);

    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    void startOrStop(Integer status, Long id);

    Employee getById(Long id);

    void edit(EmployeeDTO employeeDTO);

    void editPassword(EmployeePasswordDTO employeePasswordDTO);
}
