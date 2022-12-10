package org.synyx.urlaubsverwaltung.sicknote.sickdays;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.synyx.urlaubsverwaltung.absence.DateRange;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.period.DayLength;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonId;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.Role;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedata;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedataService;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.sicknote.sicknotetype.SickNoteType;
import org.synyx.urlaubsverwaltung.web.DateFormatAware;
import org.synyx.urlaubsverwaltung.web.FilterPeriod;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendar;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.synyx.urlaubsverwaltung.period.DayLength.FULL;
import static org.synyx.urlaubsverwaltung.period.DayLength.MORNING;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.person.Role.USER;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteCategory.SICK_NOTE;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteCategory.SICK_NOTE_CHILD;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteStatus.ACTIVE;

@ExtendWith(MockitoExtension.class)
class SickDaysOverviewViewControllerTest {

    private SickDaysOverviewViewController sut;

    @Mock
    private SickDaysStatisticsService sickDaysStatisticsService;
    @Mock
    private PersonBasedataService personBasedataService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private PersonService personService;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new SickDaysOverviewViewController(sickDaysStatisticsService, personBasedataService, departmentService,
                personService, new DateFormatAware(), clock);
    }

    @Test
    void filterSickNotes() throws Exception {

        final int year = Year.now(clock).getValue();
        final LocalDate startDate = LocalDate.parse(year + "-01-01");
        final LocalDate endDate = LocalDate.parse(year + "-12-31");
        final FilterPeriod filterPeriod = new FilterPeriod(startDate, endDate);

        perform(post("/web/sickdays/filter").flashAttr("period", filterPeriod))
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/web/sickdays?from=" + year + "-01-01&to=" + year + "-12-31"));
    }

    private static Stream<Arguments> dateInputAndIsoDateTuple() {
        return Stream.of(
            Arguments.of("25.03.2022", "2022-03-25"),
            Arguments.of("25.03.22", "2022-03-25"),
            Arguments.of("25.3.2022", "2022-03-25"),
            Arguments.of("25.3.22", "2022-03-25"),
            Arguments.of("1.4.22", "2022-04-01")
        );
    }

    @ParameterizedTest
    @MethodSource("dateInputAndIsoDateTuple")
    void applicationForLeaveStatisticsRedirectsToStatisticsAfterIncorrectPeriodForStartDate(String givenDate, String givenIsoDate) throws Exception {

        perform(post("/web/sickdays/filter")
            .param("startDate", givenDate))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/web/sickdays?from=" + givenIsoDate + "&to=2022-12-31"));
    }

    @ParameterizedTest
    @MethodSource("dateInputAndIsoDateTuple")
    void applicationForLeaveStatisticsRedirectsToStatisticsForEndDate(String givenDate, String givenIsoDate) throws Exception {

        perform(post("/web/sickdays/filter")
            .param("endDate", givenDate))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/web/sickdays?from=2022-01-01&to=" + givenIsoDate));
    }

    @Test
    void filterSickNotesWithNullDates() throws Exception {

        final int year = Year.now(clock).getValue();
        final FilterPeriod filterPeriod = new FilterPeriod(null, null);

        perform(post("/web/sickdays/filter")
            .flashAttr("period", filterPeriod))
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/web/sickdays?from=" + year + "-01-01&to=" + year + "-12-31"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OFFICE", "BOSS"})
    void periodsSickNotesWithDateRangeWithRole(Role role) throws Exception {

        final Person office = new Person();
        office.setId(1);
        office.setPermissions(List.of(USER, role));
        when(personService.getSignedInUser()).thenReturn(office);

        final Person person = new Person();
        person.setId(1);
        person.setPermissions(List.of(USER));
        final List<Person> persons = List.of(person);
        when(personService.getActivePersons()).thenReturn(persons);

        final SickNoteType childSickType = new SickNoteType();
        childSickType.setCategory(SICK_NOTE_CHILD);

        final Map<LocalDate, DayLength> workingTimes = buildWorkingTimeByDate(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), (date) -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(workingTimes);

        final SickNote childSickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 2, 1))
            .endDate(LocalDate.of(2019, 3, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(childSickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 2, 10))
            .aubEndDate(LocalDate.of(2019, 2, 15))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final SickNoteType sickType = new SickNoteType();
        sickType.setCategory(SICK_NOTE);

        final SickNote sickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 4, 1))
            .endDate(LocalDate.of(2019, 5, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(sickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 4, 10))
            .aubEndDate(LocalDate.of(2019, 4, 20))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);
        final SickDaysDetailedStatistics sickDaysDetailedStatistics = new SickDaysDetailedStatistics("", person, List.of(sickNote, childSickNote), List.of());
        when(sickDaysStatisticsService.getAll(office, requestStartDate, requestEndDate)).thenReturn(List.of(sickDaysDetailedStatistics));

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sickDaysStatistics", contains(
                allOf(
                    hasProperty("personId", is(1)),
                    hasProperty("amountSickDays", is(BigDecimal.valueOf(15))),
                    hasProperty("amountSickDaysWithAUB", is(BigDecimal.valueOf(6))),
                    hasProperty("amountChildSickDays", is(BigDecimal.valueOf(19))),
                    hasProperty("amountChildSickDaysWithAUB", is(BigDecimal.valueOf(5)))
                )
            )))
            .andExpect(model().attribute("from", requestStartDate))
            .andExpect(model().attribute("to", requestEndDate))
            .andExpect(model().attribute("period", hasProperty("startDate", is(requestStartDate))))
            .andExpect(model().attribute("period", hasProperty("endDate", is(requestEndDate))))
            .andExpect(view().name("thymeleaf/sicknote/sick_days"));
    }

    @Test
    void periodsSickNotesWithDateRangeWithDepartmentHeadRole() throws Exception {

        final Person departmentHead = new Person();
        departmentHead.setId(1);
        departmentHead.setPermissions(List.of(USER, DEPARTMENT_HEAD));
        when(personService.getSignedInUser()).thenReturn(departmentHead);

        final Person person = new Person();
        person.setId(1);
        person.setPermissions(List.of(USER));
        final List<Person> persons = List.of(person);
        when(departmentService.getMembersForDepartmentHead(departmentHead)).thenReturn(persons);

        final Map<LocalDate, DayLength> workingTimes = buildWorkingTimeByDate(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), (date) -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(workingTimes);

        final SickNoteType childSickType = new SickNoteType();
        childSickType.setCategory(SICK_NOTE_CHILD);

        final SickNote childSickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 2, 1))
            .endDate(LocalDate.of(2019, 3, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(childSickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 2, 10))
            .aubEndDate(LocalDate.of(2019, 2, 15))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final SickNoteType sickType = new SickNoteType();
        sickType.setCategory(SICK_NOTE);

        final SickNote sickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 4, 1))
            .endDate(LocalDate.of(2019, 5, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(sickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 4, 10))
            .aubEndDate(LocalDate.of(2019, 4, 20))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);
        final SickDaysDetailedStatistics sickDaysDetailedStatistics = new SickDaysDetailedStatistics("", person, List.of(sickNote, childSickNote), List.of());
        when(sickDaysStatisticsService.getAll(departmentHead, requestStartDate, requestEndDate)).thenReturn(List.of(sickDaysDetailedStatistics));

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sickDaysStatistics", contains(
                allOf(
                    hasProperty("personId", is(1)),
                    hasProperty("amountSickDays", is(BigDecimal.valueOf(15))),
                    hasProperty("amountSickDaysWithAUB", is(BigDecimal.valueOf(6))),
                    hasProperty("amountChildSickDays", is(BigDecimal.valueOf(19))),
                    hasProperty("amountChildSickDaysWithAUB", is(BigDecimal.valueOf(5)))
                )
            )))
            .andExpect(model().attribute("from", requestStartDate))
            .andExpect(model().attribute("to", requestEndDate))
            .andExpect(model().attribute("period", hasProperty("startDate", is(requestStartDate))))
            .andExpect(model().attribute("period", hasProperty("endDate", is(requestEndDate))))
            .andExpect(view().name("thymeleaf/sicknote/sick_days"));
    }

    @Test
    void periodsSickNotesWithDateRangeWithSecondStageAuthorityRole() throws Exception {

        final Person ssa = new Person();
        ssa.setId(1);
        ssa.setPermissions(List.of(USER, SECOND_STAGE_AUTHORITY));
        when(personService.getSignedInUser()).thenReturn(ssa);

        final Person person = new Person();
        person.setId(1);
        person.setPermissions(List.of(USER));
        final List<Person> persons = List.of(person);
        when(departmentService.getMembersForSecondStageAuthority(ssa)).thenReturn(persons);

        final Map<LocalDate, DayLength> workingTimes = buildWorkingTimeByDate(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), (date) -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(workingTimes);

        final SickNoteType childSickType = new SickNoteType();
        childSickType.setCategory(SICK_NOTE_CHILD);

        final SickNote childSickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 2, 1))
            .endDate(LocalDate.of(2019, 3, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(childSickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 2, 10))
            .aubEndDate(LocalDate.of(2019, 2, 15))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final SickNoteType sickType = new SickNoteType();
        sickType.setCategory(SICK_NOTE);

        final SickNote sickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 4, 1))
            .endDate(LocalDate.of(2019, 5, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(sickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 4, 10))
            .aubEndDate(LocalDate.of(2019, 4, 20))
            .workingTimeCalendar(workingTimeCalendar)
            .build();

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);
        final SickDaysDetailedStatistics sickDaysDetailedStatistics = new SickDaysDetailedStatistics("", person, List.of(sickNote, childSickNote), List.of());
        when(sickDaysStatisticsService.getAll(ssa, requestStartDate, requestEndDate)).thenReturn(List.of(sickDaysDetailedStatistics));

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sickDaysStatistics", contains(
                allOf(
                    hasProperty("personId", is(1)),
                    hasProperty("amountSickDays", is(BigDecimal.valueOf(15))),
                    hasProperty("amountSickDaysWithAUB", is(BigDecimal.valueOf(6))),
                    hasProperty("amountChildSickDays", is(BigDecimal.valueOf(19))),
                    hasProperty("amountChildSickDaysWithAUB", is(BigDecimal.valueOf(5)))
                )
            )))
            .andExpect(model().attribute("from", requestStartDate))
            .andExpect(model().attribute("to", requestEndDate))
            .andExpect(model().attribute("period", hasProperty("startDate", is(requestStartDate))))
            .andExpect(model().attribute("period", hasProperty("endDate", is(requestEndDate))))
            .andExpect(view().name("thymeleaf/sicknote/sick_days"));
    }

    @Test
    void periodsSickNotesWithDateRangeWithSecondStageAuthorityAndDepartmentRole() throws Exception {

        final Person dhAndSsa = new Person();
        dhAndSsa.setId(1);
        dhAndSsa.setPermissions(List.of(USER, DEPARTMENT_HEAD, SECOND_STAGE_AUTHORITY));
        when(personService.getSignedInUser()).thenReturn(dhAndSsa);

        final Person person = new Person();
        person.setId(1);
        person.setFirstName("FirstName");
        person.setLastName("LastName");
        person.setPermissions(List.of(USER));
        when(departmentService.getMembersForSecondStageAuthority(dhAndSsa)).thenReturn(List.of(person));

        final Map<LocalDate, DayLength> workingTimesPersonOne = buildWorkingTimeByDate(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), (date) -> FULL);
        final WorkingTimeCalendar workingTimeCalendarPersonOne = new WorkingTimeCalendar(workingTimesPersonOne);

        final SickNoteType childSickType = new SickNoteType();
        childSickType.setCategory(SICK_NOTE_CHILD);

        final SickNote childSickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 2, 1))
            .endDate(LocalDate.of(2019, 3, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(childSickType)
            .person(person)
            .aubStartDate(LocalDate.of(2019, 2, 10))
            .aubEndDate(LocalDate.of(2019, 2, 15))
            .workingTimeCalendar(workingTimeCalendarPersonOne)
            .build();

        final Person person2 = new Person();
        person2.setId(2);
        person2.setFirstName("FirstName two");
        person2.setLastName("LastName two");
        person2.setPermissions(List.of(USER));
        when(departmentService.getMembersForDepartmentHead(dhAndSsa)).thenReturn(List.of(person2));

        final Map<LocalDate, DayLength> workingTimesPersonTwo = buildWorkingTimeByDate(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), (date) -> MORNING);
        final WorkingTimeCalendar workingTimeCalendarPersonTwo = new WorkingTimeCalendar(workingTimesPersonTwo);

        final SickNoteType sickType = new SickNoteType();
        sickType.setCategory(SICK_NOTE);

        final SickNote sickNote = SickNote.builder()
            .startDate(LocalDate.of(2019, 4, 1))
            .endDate(LocalDate.of(2019, 5, 1))
            .dayLength(FULL)
            .status(ACTIVE)
            .sickNoteType(sickType)
            .person(person2)
            .aubStartDate(LocalDate.of(2019, 4, 10))
            .aubEndDate(LocalDate.of(2019, 4, 20))
            .workingTimeCalendar(workingTimeCalendarPersonTwo)
            .build();

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);
        final SickDaysDetailedStatistics sickDaysDetailedStatisticsPerson = new SickDaysDetailedStatistics("", person, List.of(childSickNote), List.of());
        final SickDaysDetailedStatistics sickDaysDetailedStatisticsPerson2 = new SickDaysDetailedStatistics("", person2, List.of(sickNote), List.of());
        when(sickDaysStatisticsService.getAll(dhAndSsa, requestStartDate, requestEndDate)).thenReturn(List.of(sickDaysDetailedStatisticsPerson, sickDaysDetailedStatisticsPerson2));

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sickDaysStatistics", contains(
                allOf(
                    hasProperty("personId", is(1)),
                    hasProperty("personAvatarUrl", is("")),
                    hasProperty("personnelNumber", nullValue()),
                    hasProperty("personFirstName", is("FirstName")),
                    hasProperty("personLastName", is("LastName")),
                    hasProperty("personNiceName", is("FirstName LastName")),
                    hasProperty("amountSickDays", is(ZERO)),
                    hasProperty("amountSickDaysWithAUB", is(ZERO)),
                    hasProperty("amountChildSickDays", is(BigDecimal.valueOf(19))),
                    hasProperty("amountChildSickDaysWithAUB", is(BigDecimal.valueOf(5)))
                ),
                allOf(
                    hasProperty("personId", is(2)),
                    hasProperty("personAvatarUrl", is("")),
                    hasProperty("personnelNumber", nullValue()),
                    hasProperty("personFirstName", is("FirstName two")),
                    hasProperty("personLastName", is("LastName two")),
                    hasProperty("personNiceName", is("FirstName two LastName two")),
                    hasProperty("amountSickDays", is(BigDecimal.valueOf(7.5))),
                    hasProperty("amountSickDaysWithAUB", is(BigDecimal.valueOf(3.0))),
                    hasProperty("amountChildSickDays", is(ZERO)),
                    hasProperty("amountChildSickDaysWithAUB", is(ZERO))
                )
            )))
            .andExpect(model().attribute("from", requestStartDate))
            .andExpect(model().attribute("to", requestEndDate))
            .andExpect(model().attribute("period", hasProperty("startDate", is(requestStartDate))))
            .andExpect(model().attribute("period", hasProperty("endDate", is(requestEndDate))))
            .andExpect(view().name("thymeleaf/sicknote/sick_days"));
    }

    @Test
    void periodsSickNotesWithDateWithoutRange() throws Exception {

        final Person office = new Person();
        office.setId(1);
        office.setPermissions(List.of(USER, OFFICE));
        when(personService.getSignedInUser()).thenReturn(office);

        final int year = Year.now(clock).getValue();
        final LocalDate startDate = ZonedDateTime.now(clock).withYear(year).with(firstDayOfYear()).toLocalDate();
        final LocalDate endDate = ZonedDateTime.now(clock).withYear(year).with(lastDayOfYear()).toLocalDate();

        final ResultActions resultActions = perform(get("/web/sickdays")
            .param("from", "01.01." + year)
            .param("to", "31.12." + year));
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(model().attribute("from", startDate));
        resultActions.andExpect(model().attribute("to", endDate));
        resultActions.andExpect(model().attribute("period", hasProperty("startDate", is(startDate))));
        resultActions.andExpect(model().attribute("period", hasProperty("endDate", is(endDate))));
        resultActions.andExpect(view().name("thymeleaf/sicknote/sick_days"));
    }

    @Test
    void sickNotesWithoutPersonnelNumberColumn() throws Exception {

        final Person signedInUser = new Person();
        signedInUser.setId(1);
        signedInUser.setPermissions(List.of(USER));
        when(personService.getSignedInUser()).thenReturn(signedInUser);

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString()))
            .andExpect(model().attribute("showPersonnelNumberColumn", false))
            .andExpect(view().name("thymeleaf/sicknote/sick_days"));
    }

    @Test
    void sickNotesWithPersonnelNumberColumn() throws Exception {

        final Person office = new Person();
        office.setId(1);
        office.setPermissions(List.of(USER, OFFICE));
        when(personService.getSignedInUser()).thenReturn(office);

        final Person person = new Person();
        person.setId(1);
        final List<Person> persons = List.of(person);
        when(personService.getActivePersons()).thenReturn(persons);
        when(personBasedataService.getBasedataByPersonId(1)).thenReturn(Optional.of(new PersonBasedata(new PersonId(1), "42", null)));

        final LocalDate requestStartDate = LocalDate.of(2019, 2, 11);
        final LocalDate requestEndDate = LocalDate.of(2019, 4, 15);

        perform(get("/web/sickdays")
            .param("from", requestStartDate.toString())
            .param("to", requestEndDate.toString()))
            .andExpect(model().attribute("sickDaysStatistics", contains(
                allOf(
                    hasProperty("personId", is(1)),
                    hasProperty("personnelNumber", is("42"))
                )
            )))
            .andExpect(model().attribute("showPersonnelNumberColumn", true))
            .andExpect(view().name("thymeleaf/sicknote/sick_days"));
    }

    private Map<LocalDate, DayLength> buildWorkingTimeByDate(LocalDate from, LocalDate to, Function<LocalDate, DayLength> dayLengthProvider) {
        Map<LocalDate, DayLength> map = new HashMap<>();
        for (LocalDate date : new DateRange(from, to)) {
            map.put(date, dayLengthProvider.apply(date));
        }
        return map;
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut).build().perform(builder);
    }
}
