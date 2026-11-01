# Лаб 08 — Mock объектууд: Mockito-гоор хамаарлыг орлуулж тестлэх

F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)

|            |                       |
| ---------- | --------------------- |
| Оюутны нэр | О.Мэнд-Амар           |
| Оюутны код | B232270001 |

## Орчин

`java -version`:

```
openjdk version "17.0.19" 2026-04-21
OpenJDK Runtime Environment Homebrew (build 17.0.19+0)
OpenJDK 64-Bit Server VM Homebrew (build 17.0.19+0, mixed mode, sharing)
```

`mvn -version`:

```
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: /opt/homebrew/Cellar/maven/3.9.16/libexec
Java version: 26.0.1, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk/26.0.1/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8
OS name: "mac os x", version: "26.6.2", arch: "aarch64", family: "mac"
```

> Тэмдэглэл: PATH дээрх `java` нь 17.0.19, харин Maven нь Homebrew-ийн JDK 26.0.1-ээр ажилладаг тул `results/` дахь тестүүд JDK 26 дээр ажилласан. `pom.xml`-д `maven.compiler.release=17` заасан тул код Java 17-ийн түвшинд хөрвүүлэгдсэн.

## Репозиторийн бүтэц

```
.
├── pom.xml                         # JUnit 5.10.2 + Mockito 5.11.0
├── src/main/java/mn/edu/must/sqat/
│   ├── Student.java                # record
│   ├── StudentRepository.java      # интерфейс — тестэд mock
│   ├── EmailSender.java            # интерфейс — тестэд mock
│   └── CourseRegistrationService.java   # тестлэгдэх класс (эх хувилбараараа)
├── src/test/java/mn/edu/must/sqat/
│   └── CourseRegistrationServiceTest.java
└── results/
    ├── mvn-test.txt                # эцсийн ногоон ажиллагаа
    ├── mvn-test-mutant-1.txt
    ├── mvn-test-mutant-2.txt
    └── mvn-test-mutant-3.txt
```

## Хэсэг A — Тестүүд

**Тестийн тоо: 10** (`results/mvn-test.txt`-ийн сүүлийн `Tests run:` мөр) — 7 энгийн тест + A8-ийн 3 мөр.

Тестийн нэрс `юуг_ямарНөхцөлд_юуХүлээгдэнэ` хэв маягтай (`register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction` гэх мэт).

| №   | Тестийн метод                                                                 | Шалгах зүйл                                                                                    | Гол хэрэгсэл                                     |
| --- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| A1  | `register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail`         | `"OK"`, `saveRegistration` дуудагдсан, и-мэйл зөв хаяг руу явсан                               | `when/thenReturn`, `verify`                      |
| A2  | `register_unknownStudent_returnsErrorNoStudentAndNeverSavesOrSends`           | `"ERROR_NO_STUDENT"`, хадгалаагүй, и-мэйл яваагүй                                              | `verify(..., never())`                           |
| A3  | `register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction`          | `"ERROR_INACTIVE_STUDENT"`, и-мэйлтэй огт харьцаагүй                                           | `verifyNoInteractions`                           |
| A4  | `register_missingPrerequisite_returnsErrorPrerequisitesAndNeverSaves`         | `"ERROR_PREREQUISITES"`, хадгалаагүй                                                           | `when` + `never()`                               |
| A5  | `register_emailSenderThrows_stillReturnsOkAndRegistrationSaved`               | и-мэйл exception шидсэн ч `"OK"`, хадгалагдсан                                                 | `doThrow(...).when(...)`                         |
| A6  | `register_nullStudentIdOrNullCourseId_throwsIllegalArgumentAndNoInteractions` | `(null, CS201)` ба `(S001, null)` тус бүр `IllegalArgumentException`; хамаарлуудтай харьцаагүй | `assertThrows`, `verifyNoInteractions`           |
| A7  | `register_success_emailBodyContainsCourseId`                                  | барьсан и-мэйлийн биед `CS201` орсон                                                           | `ArgumentCaptor`                                 |
| A8  | `register_errorClass_returnsExpectedCodeAndNeverSavesOrSends`                 | `ERROR_NO_STUDENT`, `ERROR_INACTIVE_STUDENT`, `ERROR_NO_COURSE` — 3 мөр                        | `@ParameterizedTest` + `@CsvSource`, `lenient()` |

Matcher-ийн дүрэм: нэг дуудалтад matcher хэрэглэсэн бол бүх аргументад хэрэглэсэн — жишээ нь `verify(emailSender).send(eq(EMAIL), eq(SUBJECT), contains(COURSE_ID))`, A7-д `bodyCaptor.capture()`-ийг `eq(...)`-тэй хамт.

A8-ийн гурав дахь алдааны ангид `ERROR_PREREQUISITES`-ийн оронд `ERROR_NO_COURSE`-ийг сонгосон: `ERROR_PREREQUISITES`-ийг A4 аль хэдийн шалгадаг, харин `ERROR_NO_COURSE`-ийг бусад тест хамардаггүй байсан.

### A8 дахь `lenient()`-ийн тайлбар

A8-ийн мөр бүр `register()`-ийг өөр цэг дээр зогсоодог тул нийтлэг stub-ууд бүх мөрөнд ашиглагдахгүй — жишээ нь `ERROR_NO_STUDENT` мөрөнд `courseExists()` ба `prerequisites()` огт дуудагдахгүй. `MockitoExtension`-ийн strict stubs горим ийм ашиглагдаагүй stub-ыг `UnnecessaryStubbingException` гэж унагаах тул эдгээр stub-ыг `lenient().when(...)` болгож, зөвхөн энэ параметржүүлсэн тестэд л strictness-ийг сулруулсан.

## Хэсэг B — Мини мутаци

Мутант бүрийг `CourseRegistrationService.java`-д нэг өөрчлөлт хийж, `mvn test 2>&1 | tee results/mvn-test-mutant-N.txt`-ээр ажиллуулж, дараа нь `git checkout -- src/main`-аар эх хувилбарыг сэргээсэн.

| Мутант | Өөрчлөлт (`CourseRegistrationService.java`)                                   | Унасан тест(үүд)                                                                                                                                                                                                                                 | Үр дүн (файл)                                                                               |
| ------ | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------- |
| 1      | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";` мөрийг комментолсон | **A3** `register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction` — хүлээгдсэн `ERROR_INACTIVE_STUDENT`, бодит `ERROR_NO_COURSE`; **A8[2]** `register_errorClass_...` (идэвхгүй мөр) — хүлээгдсэн `ERROR_INACTIVE_STUDENT`, бодит `OK` | Tests run: 10, **Failures: 2**, Errors: 0 — BUILD FAILURE (`results/mvn-test-mutant-1.txt`) |
| 2      | `repository.saveRegistration(studentId, courseId);` мөрийг комментолсон       | **A1** `register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail`; **A5** `register_emailSenderThrows_stillReturnsOkAndRegistrationSaved` — хоёулаа `Wanted but not invoked: repository.saveRegistration("S001", "CS201")`            | Tests run: 10, **Failures: 2**, Errors: 0 — BUILD FAILURE (`results/mvn-test-mutant-2.txt`) |
| 3      | Эхний `if`-ээс `\|\| courseId == null`-ийг устгасан                           | **A6** `register_nullStudentIdOrNullCourseId_throwsIllegalArgumentAndNoInteractions` — `Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.`                                                                       | Tests run: 10, **Failures: 1**, Errors: 0 — BUILD FAILURE (`results/mvn-test-mutant-3.txt`) |

Гурван мутантын аль нь ч `UnnecessaryStubbingException` (Errors) үүсгээгүй, бүгд assertion эсвэл `verify`-ийн Failure-ээр баригдсан. Учир нь MockitoExtension тест аль хэдийн унасан үед ашиглагдаагүй stub-ын шалгалтыг алгасдаг. Мөн A3-т зөвхөн хэрэгтэй stub-ыг (`findStudent`) тавьсан тул мутант 1-ийн үед `courseExists()` stub-гүй дуудагдаж `false` буцаасан. Үүний үр дүнд тест `ERROR_NO_COURSE` гэсэн ойлгомжтой зөрүүгээр унасан.

**Мутант 3-ын тухай.** A6-г анхнаасаа хоёр аргументын `null`-ийг тус тусад нь (`register(null, "CS201")` ба `register("S001", null)`) шалгахаар бичсэн. Тиймээс мутант эхний ажиллуулалтаар баригдсан бөгөөд A6-д засвар хийх шаардлага гараагүй. Хэрэв A6 зөвхөн `register(null, null)`-ийг шалгадаг байсан бол `studentId == null` нөхцөл дангаараа exception шидэх тул энэ мутант амьд үлдэх байсан. Энэ нь «хоёр нөхцөлийн OR»-ийг нөхцөл тус бүрийг тусад нь үнэн болгож шалгах ёстойг (MC/DC-ийн санаа) харуулж байна.

**Мутацийн дүгнэлт.** 3 мутант бүгд дор хаяж нэг тестээр баригдсан (mutation score 3/3). Мутант бүрийг 1–2 тест л барьсан нь тест бүр тодорхой нэг зан төлөвийг хариуцдагийг илтгэнэ: идэвхгүй оюутны шалгалтыг A3/A8, хадгалалтыг A1/A5, null шалгалтыг A6 хамгаалж байна. Харин A7 мутант 2-ыг бариагүй, учир нь тэр зөвхөн и-мэйлийн агуулгыг шалгадаг. Энэ нь A7-д хадгалалтын `verify`-ийг давхардуулаагүйгээс үүдэлтэй, санаатай сонголт юм.

## Дүгнэлт

Mock нь Mockito ажиллах үед динамикаар үүсгэдэг, ямар ч логикгүй орлуулагч объект бөгөөд тест түүнд яг ямар хариу өгөхийг (`when/thenReturn`) зааж, дараа нь ямар дуудалт хийгдсэнийг (`verify`) шалгадаг. Харин гараар бичсэн fake (жишээ нь `HashMap`-д суурилсан `InMemoryStudentRepository`) нь хялбаршуулсан боловч бодит ажиллагаатай хэрэгжүүлэлт бөгөөд гол төлөв гаралтын төлөвийг шалгахад (state verification) хэрэглэгддэг. Mock нь харин харилцан үйлдлийг шалгахад (interaction verification) тохиромжтой: «и-мэйл илгээгдсэн үү», «хадгалалт хийгдээгүй юу» гэх мэт гаралт буцаадаггүй гаж нөлөөг зөвхөн mock-оор батлах боломжтой. Mock хийх ёсгүй зүйлд `Student` шиг энгийн өгөгдлийн объект (record, value object), `List` зэрэг стандарт сангийн төрөл, мөн тестлэгдэж буй класс өөрөө (`CourseRegistrationService`) хамаарна. Ер нь өөрийн эзэмшдэггүй гуравдагч талын төрлийг шууд mock хийхийн оронд өөрийн интерфейсээр (энд `EmailSender`) ороох нь зүйтэй. `verify`-г хэтрүүлбэл тест хэрэгжүүлэлтийн дотоод дарааллыг (дуудалтын тоо, дараалал, туслах методууд) «цементэлж», зан төлөв өөрчлөгдөөгүй рефакторинг хийхэд л тест унадаг эмзэг тест болно. Тиймээс энэ лабораторид `verify`-г зөвхөн спецификацид шууд заасан гаж нөлөөнд (хадгалалт, и-мэйл) хэрэглэсэн бөгөөд `verifyNoMoreInteractions` болон `courseExists`/`prerequisites` зэрэг уншилтын дуудалтын верификацийг санаатайгаар ашиглаагүй. Хэсэг B-ийн мутаци нь «тест ногоон байна» гэдэг нь «тест хангалттай» гэсэн үг биш болохыг харуулсан: тест бүр кодын тодорхой эвдрэлийг барьж чадаж байж л жинхэнэ үнэ цэнэтэй болно.

## AI ашигласан тухай

Энэ лабораторийн ажилд AI туслахыг (Claude) тодорхой хэмжээнд ашигласан. Үүнд:

- **AI ашигласан хэсэг:** `CourseRegistrationServiceTest.java`-ийн анхны хувилбар, мутаци хийх `sed` командууд, README-ийн бүтэц ба тайлбаруудын ноорог.
- **Өөрөө гүйцэтгэсэн, шалгасан хэсэг:** бүх тестийг өөрийн компьютер дээр `mvn test`-ээр ажиллуулж `results/` гаралтуудыг үүсгэсэн; Хэсэг B-ийн 3 мутацийг өөрөө ажиллуулж, мутант бүрийн дараа `git checkout`-оор эх кодыг сэргээсэн; мутацийн хүснэгт ба тестийн тоо `results/` файлуудтай таарч буйг хянасан; тест бүрийн зорилго, matcher-ийн дүрэм, `lenient()`-ийн хэрэглээг ойлгож хянасан; ажлын үе шат бүрийг тусад нь commit хийсэн.

AI-ийн санал болгосон кодыг шууд хуулбарлаагүй бөгөөд Хэсэг B-ийн мутациар тестүүд кодын эвдрэлийг бодитоор барьж буйг баталгаажуулсны дараа л ашигласан.
