package nl.tudelft.sem.template.hoa.db;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.hoa.domain.Activity;
import nl.tudelft.sem.template.hoa.exception.ActivityDoesntExistException;
import nl.tudelft.sem.template.hoa.exception.BadActivityException;
import nl.tudelft.sem.template.hoa.exception.HoaDoesntExistException;
import nl.tudelft.sem.template.hoa.models.ActivityRequestModel;
import nl.tudelft.sem.template.hoa.models.MembershipResponseModel;
import nl.tudelft.sem.template.hoa.utils.MembershipUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ActivityServiceTest {

    @Autowired
    private ActivityRepo actualRepo;

    @Mock
    private ActivityRepo activityRepo;

    private HoaService hoaService;

    private ActivityService activityService;

    private static MockedStatic<MembershipUtils> membershipUtils;

    private final Activity activity = new Activity(1L, "activity 1", "description 1",
            LocalDateTime.of(2025, 12, 12, 5, 0, 0),
            LocalTime.of(2, 0, 0));

    @BeforeAll
    static void registerMocks() {
        membershipUtils = mockStatic(MembershipUtils.class);
        when(MembershipUtils.getMembershipById(1L))
                .thenReturn(new MembershipResponseModel(1L, "test user", 1L, "country", "city", false));
    }

    @AfterAll
    static void deregisterMocks() {
        membershipUtils.close();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        activityService = new ActivityService(activityRepo);
    }

    @Test
    void getActivityByIdTest() throws ActivityDoesntExistException {
        when(activityRepo.findById(anyLong())).thenReturn(Optional.of(activity));
        assertEquals(activity, activityService.getActivityById(1));
    }

    @Test
    void getActivityById_notFoundTest() {
        when(activityRepo.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ActivityDoesntExistException.class, () -> activityService.getActivityById(2L));
    }

    @Test
    void getActivitiesByHoaIdTest() {
        when(activityRepo.findByHoaId(anyLong())).thenReturn(Optional.of(List.of(activity)));
        assertEquals(List.of(activity), activityService.getActivitiesByHoaId(1));
    }

    @Test
    void joinActivity() throws ActivityDoesntExistException {
        when(activityRepo.findById(anyLong())).thenReturn(Optional.of(activity));
        Activity updatedActivity = activityService.joinActivity(1, 1);
        assertEquals(activity, updatedActivity);
        assertEquals(1, activity.getParticipants().size());
        assertTrue(activity.getParticipants().contains(1L));
    }

    @Test
    @Disabled
    void leaveActivity() throws ActivityDoesntExistException {
        when(activityRepo.findById(anyLong())).thenReturn(Optional.of(activity));
        Activity updatedActivity = activityService.joinActivity(1, 1);
        assertEquals(activity, updatedActivity);
        assertEquals(1, activity.getParticipants().size());
        assertTrue(activity.getParticipants().contains(1L));

        updatedActivity = activityService.leaveActivity(1, 1);
        assertEquals(0, activity.getParticipants().size());
        assertFalse(activity.getParticipants().contains(1L));
        assertTrue(activity.getParticipants().size() == 0);
        assertTrue(!activity.getParticipants().contains(1L));
    }

    @Test
    void updateAndRetrieveActivities() {
        Activity expiredActivity = new Activity(1L, "activity 1", "description 1",
                LocalDateTime.of(2018, 12, 12, 5, 0, 0),
                LocalTime.of(2, 0, 0));

        actualRepo.save(activity);
        actualRepo.save(expiredActivity);

        ActivityService actualService = new ActivityService(actualRepo);

        assertEquals(List.of(activity), actualService.updateAndRetrieveActivities(1L));
    }

    @Test
    @Disabled
    void createActivity() throws HoaDoesntExistException, BadActivityException {
        HoaService hoaService = mock(HoaService.class);
        when(hoaService.findHoaById(any(Long.class))).thenReturn(true);

        LocalDateTime activityTime = LocalDateTime.of(2025, 12, 12, 5, 0, 0);
        LocalTime activityDuration = LocalTime.of(2, 0, 0);
        ActivityRequestModel requestModel = new ActivityRequestModel("activity 1", "description 1",
                1L, activityTime, activityDuration);

        assertEquals(activity, activityService.createActivity(requestModel, hoaService, 1L));
        verify(activityRepo).save(activity);
    }

    @Test
    void addActivity() {
        activityService.addActivity(activity);
        verify(activityRepo).save(activity);
    }

    @Test
    void deleteActivity() {
        activityService.deleteActivity(activity);
        verify(activityRepo).delete(activity);
    }

    @Test
    void isInThisHoa() {
        assertTrue(activityService.isInThisHoa(1L, 1L));
        assertFalse(activityService.isInThisHoa(1L, 2L));
    }

    @Test
    void addActivityNullName() {
        ActivityRequestModel model = new ActivityRequestModel(null,
                "Test", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertThrows(BadActivityException.class, () -> activityService.createActivity(model, hoaService, 1L));
    }

    @Test
    void addActivityNullDescription() {
        ActivityRequestModel model = new ActivityRequestModel(
                "Test", null, 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertThrows(BadActivityException.class, () -> activityService.createActivity(model, hoaService, 1L));
    }

    @Test
    void addActivityEmptyName() {
        ActivityRequestModel model = new ActivityRequestModel("",
                "Test", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertThrows(BadActivityException.class, () -> activityService.createActivity(model, hoaService, 1L));
    }

    @Test
    void addActivityEmptyDescription() {
        ActivityRequestModel model = new ActivityRequestModel(
                "Test", "", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertThrows(BadActivityException.class, () -> activityService.createActivity(model, hoaService, 1L));
    }

    @Test
    void addActivityBlankName() {
        ActivityRequestModel model = new ActivityRequestModel("   ",
                "Test", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertThrows(BadActivityException.class, () -> activityService.createActivity(model, hoaService, 1L));
    }

    @Test
    void addActivityBlankDescription() {
        ActivityRequestModel model = new ActivityRequestModel(
                "Test", "   ", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertThrows(BadActivityException.class, () -> activityService.createActivity(model, hoaService, 1L));
    }

    @Test
    void rightFormatTitleNull() {
        assertFalse(activityService.rightFormatTitle(null));
    }

    @Test
    void rightFormatTitleEmpty() {
        assertFalse(activityService.rightFormatTitle(""));
    }

    @Test
    void rightFormatTitleBlank() {
        assertFalse(activityService.rightFormatTitle("     "));
    }

    @Test
    void rightFormatTitleHappy() {
        assertTrue(activityService.rightFormatTitle("Test123"));
    }

    @Test
    void rightFormatDescriptionNull() {
        assertFalse(activityService.rightFormatDescription(null));
    }

    @Test
    void rightFormatDescriptionEmpty() {
        assertFalse(activityService.rightFormatDescription(""));
    }

    @Test
    void rightFormatDescriptionBlank() {
        assertFalse(activityService.rightFormatDescription("     "));
    }

    @Test
    void rightFormatDescriptionHappy() {
        assertTrue(activityService.rightFormatDescription("Test123"));
    }

    @Test
    void validateNullName() {
        ActivityRequestModel model = new ActivityRequestModel(null,
                "Test", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertFalse(activityService.validateActivity(model, LocalDateTime.now()));
    }

    @Test
    void validateNullDescription() {
        ActivityRequestModel model = new ActivityRequestModel(
                "Test", null, 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertFalse(activityService.validateActivity(model, LocalDateTime.now()));
    }

    @Test
    void validateEmptyName() {
        ActivityRequestModel model = new ActivityRequestModel("",
                "Test", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertFalse(activityService.validateActivity(model, LocalDateTime.now()));
    }

    @Test
    void validateEmptyDescription() {
        ActivityRequestModel model = new ActivityRequestModel(
                "Test", "", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertFalse(activityService.validateActivity(model, LocalDateTime.now()));
    }

    @Test
    void validateBlankName() {
        ActivityRequestModel model = new ActivityRequestModel("   ",
                "Test", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertFalse(activityService.validateActivity(model, LocalDateTime.now()));
    }

    @Test
    void validateBlankDescription() {
        ActivityRequestModel model = new ActivityRequestModel(
                "Test", "   ", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertFalse(activityService.validateActivity(model, LocalDateTime.now()));
    }

    @Test
    void validateHappyCase() {
        ActivityRequestModel model = new ActivityRequestModel(
                "Test", "Test", 1L, LocalDateTime.now().plusDays(1L), LocalTime.of(2, 10));
        assertTrue(activityService.validateActivity(model, LocalDateTime.now()));
    }

}
