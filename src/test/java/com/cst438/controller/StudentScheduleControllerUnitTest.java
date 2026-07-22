package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.GradebookServiceProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudentScheduleControllerUnitTest {

    private EnrollmentRepository enrollmentRepository;
    private SectionRepository sectionRepository;
    private UserRepository userRepository;
    private GradebookServiceProxy gradebook;
    private StudentScheduleController controller;

    private Principal principal;
    private User student;
    private Section section;
    private Term term;

    @BeforeEach
    void setUp() {
        enrollmentRepository = mock(EnrollmentRepository.class);
        sectionRepository = mock(SectionRepository.class);
        userRepository = mock(UserRepository.class);
        gradebook = mock(GradebookServiceProxy.class);

        controller = new StudentScheduleController(
                enrollmentRepository,
                sectionRepository,
                userRepository,
                gradebook);

        principal = mock(Principal.class);
        student = mock(User.class);
        section = mock(Section.class);
        term = mock(Term.class);

        when(principal.getName()).thenReturn("student@csumb.edu");
        when(userRepository.findByEmail("student@csumb.edu")).thenReturn(student);
        when(student.getId()).thenReturn(10);
        when(student.getName()).thenReturn("Test Student");
        when(student.getEmail()).thenReturn("student@csumb.edu");

        when(sectionRepository.findById(5001)).thenReturn(Optional.of(section));
        when(section.getTerm()).thenReturn(term);
        when(section.getSectionNo()).thenReturn(5001);
    }

    @Test
    void addCourseCreatesAndReturnsEnrollment() throws Exception {
        setOpenAddWindow();

        Enrollment savedEnrollment = createSavedEnrollment(301);
        when(enrollmentRepository
                .findEnrollmentBySectionNoAndStudentId(5001, 10))
                .thenReturn(null);
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(savedEnrollment);

        EnrollmentDTO result = controller.addCourse(5001, principal);

        assertEquals(301, result.enrollmentId());
        assertEquals(10, result.studentId());
        assertEquals("cst438", result.courseId());
        assertEquals(5001, result.sectionNo());

        ArgumentCaptor<Enrollment> captor =
                ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());

        Enrollment created = captor.getValue();
        assertSame(student, created.getStudent());
        assertSame(section, created.getSection());
        assertNull(created.getGrade());

        verify(enrollmentRepository)
                .findEnrollmentBySectionNoAndStudentId(5001, 10);
        verifyNoInteractions(gradebook);
    }

    @Test
    void addCourseRejectsDuplicateEnrollment() {
        setOpenAddWindow();
        Enrollment existing = mock(Enrollment.class);

        when(enrollmentRepository
                .findEnrollmentBySectionNoAndStudentId(5001, 10))
                .thenReturn(existing);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.addCourse(5001, principal));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("student is already enrolled in this section",
                exception.getReason());
        verify(enrollmentRepository, never()).save(any());
        verifyNoInteractions(gradebook);
    }

    @Test
    void addCourseRejectsDateBeforeAddDate() {
        long now = System.currentTimeMillis();
        when(term.getAddDate()).thenReturn(new Date(now + 86_400_000L));
        when(term.getAddDeadline()).thenReturn(new Date(now + 172_800_000L));
        when(enrollmentRepository
                .findEnrollmentBySectionNoAndStudentId(5001, 10))
                .thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.addCourse(5001, principal));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("course cannot be added before the add date",
                exception.getReason());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void addCourseRejectsPassedAddDeadline() {
        long now = System.currentTimeMillis();
        when(term.getAddDate()).thenReturn(new Date(now - 172_800_000L));
        when(term.getAddDeadline()).thenReturn(new Date(now - 86_400_000L));
        when(enrollmentRepository
                .findEnrollmentBySectionNoAndStudentId(5001, 10))
                .thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.addCourse(5001, principal));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("add deadline has passed", exception.getReason());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void dropCourseDeletesEnrollmentOwnedByStudent() throws Exception {
        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.getStudent()).thenReturn(student);
        when(enrollment.getSection()).thenReturn(section);
        when(enrollmentRepository.findById(301))
                .thenReturn(Optional.of(enrollment));
        when(term.getDropDeadline()).thenReturn(
                new Date(System.currentTimeMillis() + 86_400_000L));

        controller.dropCourse(301, principal);

        verify(enrollmentRepository).delete(enrollment);
        verifyNoInteractions(gradebook);
    }

    @Test
    void dropCourseRejectsEnrollmentOwnedByAnotherStudent() {
        User otherStudent = mock(User.class);
        when(otherStudent.getId()).thenReturn(99);

        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.getStudent()).thenReturn(otherStudent);
        when(enrollmentRepository.findById(301))
                .thenReturn(Optional.of(enrollment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.dropCourse(301, principal));

        assertEquals(403, exception.getStatusCode().value());
        assertEquals("enrollment does not belong to the logged-in student",
                exception.getReason());
        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    void dropCourseRejectsPassedDropDeadline() {
        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.getStudent()).thenReturn(student);
        when(enrollment.getSection()).thenReturn(section);
        when(enrollmentRepository.findById(301))
                .thenReturn(Optional.of(enrollment));
        when(term.getDropDeadline()).thenReturn(
                new Date(System.currentTimeMillis() - 86_400_000L));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.dropCourse(301, principal));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("drop deadline has passed", exception.getReason());
        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    void dropCourseRejectsMissingEnrollment() {
        when(enrollmentRepository.findById(301))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.dropCourse(301, principal));

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("enrollment not found", exception.getReason());
        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    void getSectionEnrollmentsReturnsRosterForAssignedInstructor() {
        Principal instructorPrincipal = mock(Principal.class);
        User instructor = mock(User.class);
        Enrollment enrollment = createSavedEnrollment(301);

        when(instructorPrincipal.getName()).thenReturn("instructor@csumb.edu");
        when(userRepository.findByEmail("instructor@csumb.edu"))
                .thenReturn(instructor);
        when(instructor.getEmail()).thenReturn("instructor@csumb.edu");
        when(section.getInstructorEmail()).thenReturn("instructor@csumb.edu");
        when(enrollmentRepository
                .findEnrollmentsBySectionNoOrderByStudentId(5001))
                .thenReturn(List.of(enrollment));

        List<EnrollmentDTO> result = controller.getSectionEnrollments(
                5001,
                instructorPrincipal);

        assertEquals(1, result.size());
        assertEquals(301, result.get(0).enrollmentId());
        assertEquals(10, result.get(0).studentId());
        verify(enrollmentRepository)
                .findEnrollmentsBySectionNoOrderByStudentId(5001);
    }

    @Test
    void getSectionEnrollmentsRejectsSectionAssignedToAnotherInstructor() {
        Principal instructorPrincipal = mock(Principal.class);
        User instructor = mock(User.class);

        when(instructorPrincipal.getName()).thenReturn("instructor@csumb.edu");
        when(userRepository.findByEmail("instructor@csumb.edu"))
                .thenReturn(instructor);
        when(instructor.getEmail()).thenReturn("instructor@csumb.edu");
        when(section.getInstructorEmail()).thenReturn("other@csumb.edu");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getSectionEnrollments(
                        5001,
                        instructorPrincipal));

        assertEquals(403, exception.getStatusCode().value());
        assertEquals("section does not belong to the logged-in instructor",
                exception.getReason());
        verify(enrollmentRepository, never())
                .findEnrollmentsBySectionNoOrderByStudentId(anyInt());
    }

    @Test
    void getSectionEnrollmentsRejectsMissingSection() {
        Principal instructorPrincipal = mock(Principal.class);
        User instructor = mock(User.class);

        when(instructorPrincipal.getName()).thenReturn("instructor@csumb.edu");
        when(userRepository.findByEmail("instructor@csumb.edu"))
                .thenReturn(instructor);
        when(sectionRepository.findById(9999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getSectionEnrollments(
                        9999,
                        instructorPrincipal));

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("section not found", exception.getReason());
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void getSectionEnrollmentsRejectsMissingPrincipal() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getSectionEnrollments(5001, null));

        assertEquals(401, exception.getStatusCode().value());
        assertEquals("instructor is not authenticated", exception.getReason());
        verifyNoInteractions(enrollmentRepository);
    }

    private void setOpenAddWindow() {
        long now = System.currentTimeMillis();
        when(term.getAddDate()).thenReturn(new Date(now - 86_400_000L));
        when(term.getAddDeadline()).thenReturn(new Date(now + 86_400_000L));
    }

    private Enrollment createSavedEnrollment(int enrollmentId) {
        Enrollment enrollment = mock(Enrollment.class);
        Course course = mock(Course.class);

        when(enrollment.getEnrollmentId()).thenReturn(enrollmentId);
        when(enrollment.getGrade()).thenReturn(null);
        when(enrollment.getStudent()).thenReturn(student);
        when(enrollment.getSection()).thenReturn(section);

        when(section.getCourse()).thenReturn(course);
        when(section.getSectionId()).thenReturn(1);
        when(section.getBuilding()).thenReturn("BIT");
        when(section.getRoom()).thenReturn("104");
        when(section.getTimes()).thenReturn("MW 10:00-11:50");

        when(course.getCourseId()).thenReturn("cst438");
        when(course.getTitle()).thenReturn("Software Engineering");
        when(course.getCredits()).thenReturn(4);

        when(term.getYear()).thenReturn(2026);
        when(term.getSemester()).thenReturn("Fall");

        return enrollment;
    }
}
