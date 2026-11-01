package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CourseRegistrationService-ийн нэгжийн тест.
 * StudentRepository ба EmailSender хоёулаа Mockito mock — өгөгдлийн сан,
 * SMTP сервер огт шаардлагагүй, зөвхөн бизнес логик тусгаарлагдан шалгагдана.
 */
@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "S001";
    private static final String EMAIL = "s001@must.edu.mn";
    private static final String COURSE_ID = "CS201";
    private static final String PREREQ = "CS101";
    private static final String SUBJECT = "Бүртгэл баталгаажлаа";

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // ---- туслах аргууд --------------------------------------------------

    private static Student activeStudent(List<String> coursesTaken) {
        return new Student(STUDENT_ID, EMAIL, true, coursesTaken);
    }

    private static Student inactiveStudent() {
        return new Student(STUDENT_ID, EMAIL, false, List.of(PREREQ));
    }

    /** Бүх шалгалтыг давах "аз жаргалтай зам"-ын stub. */
    private void stubHappyPath() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of(PREREQ)));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQ));
    }

    // ---- A1 ------------------------------------------------------------

    @Test
    @DisplayName("A1: идэвхтэй оюутан → OK, хадгалагдаж, и-мэйл илгээгдэнэ")
    void register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail() {
        stubHappyPath();

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), contains(COURSE_ID));
    }

    // ---- A2 ------------------------------------------------------------

    @Test
    @DisplayName("A2: байхгүй оюутан → ERROR_NO_STUDENT, хадгалалт ба и-мэйл байхгүй")
    void register_unknownStudent_returnsErrorNoStudentAndNeverSavesOrSends() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // ---- A3 ------------------------------------------------------------

    @Test
    @DisplayName("A3: идэвхгүй оюутан → ERROR_INACTIVE_STUDENT, и-мэйлтэй огт харьцахгүй")
    void register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(inactiveStudent());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // ---- A4 ------------------------------------------------------------

    @Test
    @DisplayName("A4: өмнөх холбоо хичээл дутуу → ERROR_PREREQUISITES, хадгалахгүй")
    void register_missingPrerequisite_returnsErrorPrerequisitesAndNeverSaves() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQ));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // ---- A5 ------------------------------------------------------------

    @Test
    @DisplayName("A5: и-мэйл илгээгч exception шидсэн ч OK, бүртгэл хадгалагдсан")
    void register_emailSenderThrows_stillReturnsOkAndRegistrationSaved() {
        stubHappyPath();
        doThrow(new RuntimeException("SMTP сервер унасан"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    // ---- A6 ------------------------------------------------------------

    @Test
    @DisplayName("A6: studentId эсвэл courseId null → IllegalArgumentException, хамаарлуудтай харьцахгүй")
    void register_nullStudentIdOrNullCourseId_throwsIllegalArgumentAndNoInteractions() {
        // Аргумент тус бүрийн null-ийг ТУСАД НЬ шалгана: зөвхөн (null, null)
        // шалгавал "|| courseId == null"-ийг устгасан мутант амьд үлдэнэ.
        assertThrows(IllegalArgumentException.class,
                () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(STUDENT_ID, null));

        verifyNoInteractions(repository, emailSender);
    }

    // ---- A7 ------------------------------------------------------------

    @Test
    @DisplayName("A7: илгээсэн и-мэйлийн хаяг, гарчиг, биед хичээлийн код орсон")
    void register_success_emailBodyContainsCourseId() {
        stubHappyPath();
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        service.register(STUDENT_ID, COURSE_ID);

        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), bodyCaptor.capture());
        String body = bodyCaptor.getValue();
        assertTrue(body.contains(COURSE_ID),
                () -> "И-мэйлийн биед " + COURSE_ID + " байх ёстой, бодит: " + body);
    }

    // ---- A8 ------------------------------------------------------------

    @ParameterizedTest(name = "A8[{index}] studentExists={0}, active={1}, courseExists={2} → {3}")
    @CsvSource({
            "false, true,  true,  ERROR_NO_STUDENT",
            "true,  false, true,  ERROR_INACTIVE_STUDENT",
            "true,  true,  false, ERROR_NO_COURSE"
    })
    void register_errorClass_returnsExpectedCodeAndNeverSavesOrSends(
            boolean studentExists, boolean active, boolean courseExists, String expected) {
        Student student = studentExists
                ? new Student(STUDENT_ID, EMAIL, active, List.of(PREREQ))
                : null;

        // Мөр бүр кодыг өөр цэг дээр зогсоодог тул бүх stub мөр бүрт ашиглагдахгүй
        // (жишээ нь ERROR_NO_STUDENT мөрөнд courseExists/prerequisites дуудагдахгүй).
        // Strict stubs үүнийг UnnecessaryStubbingException гэж үзэх тул lenient().
        lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(student);
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(courseExists);
        lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
