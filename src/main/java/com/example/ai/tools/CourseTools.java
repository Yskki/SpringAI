package com.example.ai.tools;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.example.ai.entity.po.Course;
import com.example.ai.entity.po.CourseReservation;
import com.example.ai.entity.po.School;
import com.example.ai.entity.query.CourseQuery;
import com.example.ai.service.ICourseReservationService;
import com.example.ai.service.ICourseService;
import com.example.ai.service.ISchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CourseTools
{
    private final ICourseService courseService;
    private final ISchoolService schoolService;
    private final ICourseReservationService reservationService;
    @Tool(description = "根据条件查询课程")//标记是一个Tool方法，未来大模型可能需要调用
    //加上required = false表示这个是可选字段，不是必须的
    public List<Course> queryCourse(@ToolParam(description = "查询的条件",required = false) CourseQuery query)
    {
        if(query == null) {
            return courseService.list();//这里查询条件为空，可以返回所有课程，也可以返回空，这里选择返回所有课程
        }
        QueryChainWrapper<Course> wrapper = courseService.query()
                .eq(query.getType() != null, "type", query.getType()) //不为null才进行比较,type = ?
                .le(query.getEdu() != null, "edu", query.getEdu());//同上，这里是edu <= ?
        if(query.getSorts() != null && !query.getSorts().isEmpty())
        {
            for (CourseQuery.Sort sort : query.getSorts()) {
                //第一个参数表示是否要启用这个排序规则
                //第二个参数表示是否是升序
                //第三个参数表示排序的字段
                wrapper.orderBy(true,sort.getAsc(),sort.getField());
            }
        }
        return wrapper.list();
    }

    @Tool(description = "查询所有校区")
    public List<School> querySchool(){
        return schoolService.list();//查询所有的校区
    }

    @Tool(description = "生成预约单，返回预约单号")
    public Integer createCourseReservation(
            @ToolParam(description = "预约课程") String course,
            @ToolParam(description = "预约校区") String school,
            @ToolParam(description = "学生姓名") String studentName,
            @ToolParam(description = "联系方式") String contactInfo,
            @ToolParam(description = "备注",required = false) String remark) {
        CourseReservation reservation = new CourseReservation();
        reservation.setCourse(course);
        reservation.setSchool(school);
        reservation.setStudentName(studentName);
        reservation.setContactInfo(contactInfo);
        reservation.setRemark(remark);
        reservationService.save(reservation);
        return reservation.getId();//上边保存之后，这里id会进行回显
    }
}
