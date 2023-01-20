package nl.tudelft.sem.template.hoa.controllers;

import static nl.tudelft.sem.template.hoa.annotations.TestSuite.TestType.INTEGRATION;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.tudelft.sem.template.hoa.annotations.TestSuite;
import nl.tudelft.sem.template.hoa.db.HoaRepo;
import nl.tudelft.sem.template.hoa.db.HoaService;
import nl.tudelft.sem.template.hoa.domain.Hoa;
import nl.tudelft.sem.template.hoa.models.BoardElectionRequestModel;
import nl.tudelft.sem.template.hoa.models.HoaRequestModel;
import nl.tudelft.sem.template.hoa.models.MembershipResponseModel;
import nl.tudelft.sem.template.hoa.models.TimeModel;
import nl.tudelft.sem.template.hoa.utils.ElectionUtils;
import nl.tudelft.sem.template.hoa.utils.JsonUtil;
import nl.tudelft.sem.template.hoa.utils.MembershipUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestSuite(testType = INTEGRATION)
public class HoaControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private transient HoaRepo hoaRepo;

    @Mock
    private transient HoaService hoaService;

    private static MockedStatic<ElectionUtils> electionUtilsMockedStatic;

    private static MockedStatic<TimeModel> timeModelMockedStatic;

    private static MockedStatic<MembershipUtils> membershipUtilsMockedStatic;


    @Autowired
    private transient HoaController hoaController;

    private transient Long l1 = 1L;
    private transient Long l2 = 2L;

    private static String token = "randomToken123";

    void insertHoa() {
        Hoa hoa = Hoa.createHoa("Country", "City", "Test");
        hoaRepo.save(hoa);
    }

    @BeforeAll
    static void setupStatic() {
        BoardElectionRequestModel be = new BoardElectionRequestModel(1, 2, List.of(),
                "Annual board election", "This is the auto-generated annual board election",
                new TimeModel(10, 10, 10, 10, 10, 10));

        electionUtilsMockedStatic = Mockito.mockStatic(ElectionUtils.class);
        when(ElectionUtils.createBoardElection(any(BoardElectionRequestModel.class)))
                .thenAnswer(i -> {
                    BoardElectionRequestModel arg = (BoardElectionRequestModel) i.getArguments()[0];
                    return arg.scheduledFor == null ? null : be;
                });

        timeModelMockedStatic = Mockito.mockStatic(TimeModel.class);
        when(TimeModel.createModelFromArr(any(Integer[].class)))
                .thenAnswer(i -> {
                    Integer[] arg = (Integer[]) i.getArguments()[0];
                    if (arg[2] == LocalDateTime.now().plusDays(6).getDayOfMonth()) return null;
                    else return new TimeModel(arg[5], arg[4], arg[3], arg[2], arg[1], arg[0]);
                });

        membershipUtilsMockedStatic = Mockito.mockStatic(MembershipUtils.class);
        when(MembershipUtils.getActiveMembershipsForUser(anyString(), eq(token)))
                .thenAnswer(i -> {
                    String memberId = (String) i.getArguments()[0];
                    return List.of(new MembershipResponseModel(1, "memberOne", 1,
                            "a", "b", false, LocalDateTime.now(), Duration.ZERO));
                });
    }

    @AfterAll
    static void deregisterMocks() {
        electionUtilsMockedStatic.close();
        timeModelMockedStatic.close();
        membershipUtilsMockedStatic.close();
    }

    @AfterEach
    void flushRepo() {
        hoaRepo.deleteAll();
    }

    @Test
    public void registerHoaTestHappy() throws Exception {
        HoaRequestModel request = new HoaRequestModel("Country", "City", "Name");
        ResultActions resultActions = mockMvc.perform(post("/hoa/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.serialize(request)));
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(jsonPath("$.country").isString());
        resultActions.andExpect(jsonPath("$.city").isString());
        resultActions.andExpect(jsonPath("$.name").isString());
        MvcResult result = resultActions.andExpect(jsonPath("$.id").isNumber()).andReturn();
        Hoa expected = JsonUtil.deserialize(result.getResponse().getContentAsString(), Hoa.class);
        assertTrue(hoaRepo.findById(l1).isPresent());
        Hoa actual = hoaRepo.findById(l1).get();
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(JsonUtil.serialize(expected), JsonUtil.serialize(actual));
    }

    @Test
    public void registerHoaScheduledTestHappy() throws Exception {
        HoaRequestModel request = new HoaRequestModel("Country", "City", "Name");
        ResultActions resultActions = mockMvc.perform(post("/hoa/create/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.serialize(request)));
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(jsonPath("$.country").isString());
        resultActions.andExpect(jsonPath("$.city").isString());
        resultActions.andExpect(jsonPath("$.name").isString());
        MvcResult result = resultActions.andExpect(jsonPath("$.id").isNumber()).andReturn();
        Hoa expected = JsonUtil.deserialize(result.getResponse().getContentAsString(), Hoa.class);
        assertTrue(hoaRepo.findById(l1).isPresent());
        Hoa actual = hoaRepo.findById(l1).get();
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(JsonUtil.serialize(expected), JsonUtil.serialize(actual));
    }

    @Test
    public void registerHoaScheduledNegativeTimeTestHappy() throws Exception {
        HoaRequestModel request = new HoaRequestModel("Country", "City", "Name");
        ResultActions resultActions = mockMvc.perform(post("/hoa/create/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.serialize(request)));
        resultActions.andExpect(status().isOk());
    }

    @Test
    public void registerHoaTestBad() throws Exception {
        HoaRequestModel request = new HoaRequestModel("Cou$", "C13ty", "  ");
        ResultActions resultActions = mockMvc.perform(post("/hoa/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.serialize(request)));
        resultActions.andExpect(status().isBadRequest());
        assertTrue(hoaRepo.findById(l1).isEmpty());
    }

    @Test
    public void registerHoaScheduledTestBad() throws Exception {
        HoaRequestModel request = new HoaRequestModel("Country", "City", "Name");
        ResultActions resultActions = mockMvc.perform(post("/hoa/create/6")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.serialize(request)));
        resultActions.andExpect(status().isBadRequest());
        Assertions.assertFalse(hoaRepo.findById(l1).isEmpty());
    }

    @Test
    public void getAllEmpty() throws Exception {
        ResultActions resultActions = mockMvc.perform(get("/hoa/getAll")
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isOk());
        MvcResult result = resultActions.andReturn();
        List<Hoa> actual = Arrays.asList(JsonUtil.deserialize(result.getResponse().getContentAsString(), Hoa[].class));
        List<Hoa> expected = new ArrayList<>();
        Assertions.assertEquals(actual, expected);
    }

    @Test
    public void getAllOne() throws Exception {
        insertHoa();
        ResultActions resultActions = mockMvc.perform(get("/hoa/getAll")
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isOk());
        MvcResult result = resultActions.andReturn();
        final List<Hoa> actual = Arrays.asList(JsonUtil.deserialize(result.getResponse().getContentAsString(), Hoa[].class));
        List<Hoa> expected = new ArrayList<>();
        assertTrue(hoaRepo.findById(l1).isPresent());
        expected.add(hoaRepo.findById(l1).get());
        Assertions.assertEquals(expected.size(), 1);
        Assertions.assertEquals(actual.size(), 1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void getAllMultiple() throws Exception {
        insertHoa();
        Hoa anotherHoa = Hoa.createHoa("Other", "Other", "Other");
        hoaRepo.save(anotherHoa);
        ResultActions resultActions = mockMvc.perform(get("/hoa/getAll")
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isOk());
        MvcResult result = resultActions.andReturn();
        final List<Hoa> actual = Arrays.asList(JsonUtil.deserialize(result.getResponse().getContentAsString(), Hoa[].class));
        List<Hoa> expected = new ArrayList<>();
        assertTrue(hoaRepo.findById(l1).isPresent());
        expected.add(hoaRepo.findById(l1).get());
        assertTrue(hoaRepo.findById(l2).isPresent());
        expected.add(hoaRepo.findById(l2).get());
        Assertions.assertEquals(expected.size(), 2);
        Assertions.assertEquals(actual.size(), 2);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void getAllBad() throws Exception {
        when(this.hoaService.getAllHoa()).thenThrow(new IllegalArgumentException());
        hoaController.setHoaService(this.hoaService);
        ResultActions resultActions = mockMvc.perform(get("/hoa/getAll")
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    void getByIdHappy() throws Exception {
        insertHoa();
        ResultActions resultActions = mockMvc.perform(get("/hoa/getById/" + l1)
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(jsonPath("$.id").isNumber());
        resultActions.andExpect(jsonPath("$.country").isString());
        resultActions.andExpect(jsonPath("$.city").isString());
        resultActions.andExpect(jsonPath("$.name").isString());
        Hoa actual = JsonUtil.deserialize(resultActions.andReturn().getResponse().getContentAsString(), Hoa.class);
        assertTrue(hoaRepo.findById(l1).isPresent());
        Hoa expected = hoaRepo.findById(l1).get();
        Assertions.assertEquals(actual, expected);
        Assertions.assertEquals(actual.getCountry(), expected.getCountry());
        Assertions.assertEquals(actual.getCity(), expected.getCity());
        Assertions.assertEquals(actual.getName(), expected.getName());
    }

    @Test
    void getByIdBad() throws Exception {
        ResultActions resultActions = mockMvc.perform(get("/hoa/getById/" + l1)
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isBadRequest());
        assertTrue(hoaRepo.findById(l1).isEmpty());
    }

    @Test
    void getNotificationsSuccessTest() throws Exception {
        Hoa test = Hoa.createHoa("a", "b", "testHOA");
        test.notify("memberOne", "Notification1");
        test.notify("memberOne", "Notification2");
        hoaRepo.save(test);

        ResultActions resultActions = mockMvc.perform(get("/hoa/getNotifications/memberOne/" + l1)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isOk());
        String res = resultActions.andReturn().getResponse().getContentAsString();
        assertTrue(res.contains("Notification1"));
        assertTrue(res.contains("Notification2"));

        assertTrue(hoaRepo.findById(l1).isPresent());
        Hoa hoa = hoaRepo.findById(l1).get();
        System.out.println(hoa.getNotifications());
    }

    @Test
    void getNotificationsUnauthorizedTest() throws Exception {
        Hoa test = Hoa.createHoa("a", "b", "testHOA");
        test.notify("memberOne", "Notification1");
        test.notify("memberOne", "Notification2");
        hoaRepo.save(test);

        ResultActions resultActions = mockMvc.perform(get("/hoa/getNotifications/memberOne/" + l2)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isUnauthorized());
    }

}