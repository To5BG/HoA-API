package nl.tudelft.sem.template.hoa.controllers;

import java.util.ArrayList;
import java.util.List;
import nl.tudelft.sem.template.hoa.db.ActivityService;
import nl.tudelft.sem.template.hoa.db.HoaService;
import nl.tudelft.sem.template.hoa.domain.Activity;
import nl.tudelft.sem.template.hoa.exception.HoaDoesntExistException;
import nl.tudelft.sem.template.hoa.models.ActivityRequestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class ActivityController {
    private final transient ActivityService activityService;
    private final transient HoaService hoaService;

    /**
     * Constructor to create an activity controller.
     *
     * @param activityService the activity service
     */
    @Autowired
    public ActivityController(ActivityService activityService, HoaService hoaService) {
        this.activityService = activityService;
        this.hoaService = hoaService;
    }

    /**
     * Endpoint for joining an activity.
     *
     * @param membershipId the membership id
     * @param activityId   the id of the activity
     * @return a response entity
     */
    @PutMapping("/activity/join/{membershipId}/{activityId}")
    public ResponseEntity<Activity> joinActivity(@PathVariable long membershipId, @PathVariable long activityId) {
        try {
            Activity activity = activityService.getActivityById(activityId);
            activity.joinActivity(membershipId);
            activityService.addActivity(activity);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        //Optional<Membership> membership = membershipRepo.findById(memberIdl);
        // if membership is not found => not found response
        // if found, we then have hoaId, and memberId
        // check if memberId is in that Hoa
        // check if activityId is in that hoa's public board

    }

    /**
     * Endpoint for leaving an activity.
     *
     * @param membershipId the membership id
     * @param activityId   the id of the activity
     * @return a response entity
     */
    @DeleteMapping("/activity/leave/{membershipId}/{activityId}")
    public ResponseEntity<Activity> leaveActivity(@PathVariable long membershipId, @PathVariable long activityId) {
        try {
            Activity activity = activityService.getActivityById(activityId);
            activity.leaveActivity(membershipId);
            activityService.addActivity(activity);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        //Optional<Membership> membership = membershipRepo.findById(memberIdl);
        // if membership is not found => not found response
        // if found, we then have hoaId, and memberId
        // check if memberId is in that Hoa
        // check if activityId is in that hoa's public board
    }

    /**
     * Endpoint for retrieving the public board for a specific hoa.
     *
     * @param hoaId        the hoaId
     * @param membershipId the membership id of the member requesting
     * @return a response entity with all the activities within the public board
     */
    @GetMapping("/activity/publicBoard/{hoaId}/{membershipId}")
    public ResponseEntity<List<Activity>> getPublicBoard(@PathVariable long hoaId,
                                                                      @PathVariable long membershipId) {
        try {
            List<Activity> activities = activityService.getActivitiesByHoaId(hoaId);
            // check if member is actually part of this hoa
            for (Activity activity : activities) {
                if (activity.isExpired()) {
                    activityService.deleteActivity(activity);
                }
            }
            activities = activityService.getActivitiesByHoaId(hoaId);
            return ResponseEntity.ok(activities);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Endpoint for creating an activity for a specific HOA.
     *
     * @return a response entity with all the new activity
     */
    @PostMapping("/activity/create")
    public ResponseEntity<Activity>
        createActivity(@RequestBody ActivityRequestModel activityRequestModel) throws HoaDoesntExistException {

        if (hoaService.findHoaById(activityRequestModel.getHoaId())) {
            Activity activity = new Activity(activityRequestModel.getHoaId(), activityRequestModel.getActivityName(),
                    activityRequestModel.getActivityDescription(), activityRequestModel.getActivityTime(),
                    activityRequestModel.getActivityDuration());
            activityService.addActivity(activity);
            return ResponseEntity.ok(activity);
        }
        throw new HoaDoesntExistException("Hoa with id " + activityRequestModel.getHoaId() + " doesn't exist");
    }
}