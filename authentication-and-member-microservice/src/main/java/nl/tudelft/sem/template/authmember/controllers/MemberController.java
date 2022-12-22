package nl.tudelft.sem.template.authmember.controllers;

import java.util.List;

import nl.tudelft.sem.template.authmember.authentication.AuthManager;
import nl.tudelft.sem.template.authmember.authentication.JwtTokenGenerator;
import nl.tudelft.sem.template.authmember.authentication.JwtUserDetailsService;

import nl.tudelft.sem.template.authmember.domain.Member;
import nl.tudelft.sem.template.authmember.domain.Membership;
import nl.tudelft.sem.template.authmember.domain.converters.MembershipConverter;
import nl.tudelft.sem.template.authmember.domain.db.MemberService;
import nl.tudelft.sem.template.authmember.domain.db.MembershipService;
import nl.tudelft.sem.template.authmember.domain.exceptions.BadJoinHoaModelException;
import nl.tudelft.sem.template.authmember.domain.exceptions.BadRegistrationModelException;
import nl.tudelft.sem.template.authmember.domain.exceptions.MemberAlreadyExistsException;
import nl.tudelft.sem.template.authmember.domain.exceptions.MemberAlreadyInHoaException;
import nl.tudelft.sem.template.authmember.domain.exceptions.MemberDifferentAddressException;
import nl.tudelft.sem.template.authmember.models.JoinHoaModel;
import nl.tudelft.sem.template.authmember.models.RegistrationModel;
import nl.tudelft.sem.template.authmember.models.MembershipResponseModel;
import nl.tudelft.sem.template.authmember.models.GetHoaModel;
import nl.tudelft.sem.template.authmember.models.HoaModel;
import nl.tudelft.sem.template.authmember.models.AuthenticationResponseModel;
import nl.tudelft.sem.template.authmember.models.AuthenticationRequestModel;
import nl.tudelft.sem.template.authmember.services.HoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/member")
public class MemberController {

    private transient MemberService memberService;
    private transient HoaService hoaService;
    private transient MembershipService membershipService;

    private final transient AuthenticationManager authenticationManager;
    private final transient JwtTokenGenerator jwtTokenGenerator;
    private final transient JwtUserDetailsService jwtUserDetailsService;

    private transient AuthManager authManager;
    private final transient String unauthorizedMessage = "Access is not allowed";

    private static final String secretClearBoardKey = "Thisisacustomseckeyforclear";

    private static final String secretPromotionKey = "Thisisacustomseckeyforpromotion";

    /**
     * Instantiates a new MemberController.
     */
    @Autowired
    public MemberController(MemberService memberService, HoaService hoaService,
                            MembershipService membershipService,
                            AuthenticationManager authenticationManager, JwtTokenGenerator jwtTokenGenerator,
                            JwtUserDetailsService jwtUserDetailsService, AuthManager authManager) {
        this.membershipService = membershipService;
        this.memberService = memberService;
        this.hoaService = hoaService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.authManager = authManager;
    }

    /**
     * Register a new member.
     */
    @PostMapping("/register")
    public ResponseEntity<Member> register(@RequestBody RegistrationModel request) {
        try {
            memberService.registerUser(request);
            return ResponseEntity.ok().build();
        } catch (MemberAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member already exists", e);
        } catch (BadRegistrationModelException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad username or password!", e);
        }
    }


    /**
     * Change member's password.
     */
    @PostMapping("/updatePassword")
    public ResponseEntity<Member> updatePassword(@RequestBody RegistrationModel request) {
        try {
            authManager.validateMember(request.getMemberId());
            Member member = memberService.updatePassword(request);
            return ResponseEntity.ok(member);
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member doesn't exist", e);
        } catch (BadRegistrationModelException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad registration model", e);
        }
    }

    /**
     * Get a member by its id.
     *
     * @param memberId the member id
     * @return the member
     */
    @GetMapping("/get/{memberId}")
    public ResponseEntity<Member> getMember(@PathVariable String memberId) {
        try {
            authManager.validateMember(memberId);
            Member member = memberService.getMember(memberId);
            return ResponseEntity.ok(member);
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member does not exist", e);
        }
    }

    /**
     * Adds user to the HOA and creates a new Membership if succeeds.
     * Only succeeds if a user exists and doesn't have an active membership in the HOA.
     */
    @PostMapping("/joinHOA")
    public ResponseEntity<Membership> joinHoa(@RequestBody JoinHoaModel model,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        try {
            authManager.validateMember(model.getMemberId());
            hoaService.joinHoa(model, token);
            return ResponseEntity.ok().build();
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (MemberAlreadyInHoaException | IllegalArgumentException | MemberDifferentAddressException
                 | BadJoinHoaModelException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Deactivates a user's membership in the HOA.
     * Only succeeds if a member exists and has an active membership.
     */
    @PostMapping("/leaveHOA")
    public ResponseEntity<Membership> leaveHoa(@RequestBody GetHoaModel model) {
        validateExistence(model);
        try {
            authManager.validateMember(model.getMemberId());
            Membership membership = hoaService.leaveHoa(model);
            return ResponseEntity.ok(membership);
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hoa or member are not found.", e);
        }
    }

    /**
     * Gets current (active) membership in HOA.
     */
    @GetMapping("/getActiveMembership")
    public ResponseEntity<Membership> getMembership(@RequestBody GetHoaModel model) {
        validateExistence(model);
        try {
            authManager.validateMember(model.getMemberId());
            Membership membership = hoaService.getCurrentMembership(model.getMemberId(), model.getHoaId());
            return ResponseEntity.ok(membership);
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HOA or member are not stored", e);
        }
    }

    /**
     * Endpoint to retrieve a membership by id.
     *
     * @param membershipId the membership id.
     * @return the membership with the id provided
     */
    @GetMapping("/getMembershipById/{membershipId}")
    public ResponseEntity<MembershipResponseModel> getMembershipById(@PathVariable long membershipId) {
        try {
            Membership membership = membershipService.getMembership(membershipId);
            MembershipResponseModel model = MembershipConverter.convert(membership);
            return ResponseEntity.ok(model);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Returns all memberships for a given HOA (including inactive).
     */
    @GetMapping("/getMembershipsForHOA")
    public ResponseEntity<List<Membership>> getMembershipsForHoa(@RequestBody GetHoaModel model) {
        validateExistence(model);
        try {
            authManager.validateMember(model.getMemberId());
            List<Membership> memberships = hoaService.getMembershipsForHoa(model.getMemberId(), model.getHoaId());
            return ResponseEntity.ok(memberships);
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HOA or member do not exist", e);
        }
    }

    /**
     * Returns all memberships for user (including inactive).
     */
    @GetMapping("/getMemberships/{memberId}")
    public ResponseEntity<List<MembershipResponseModel>> getMemberships(@PathVariable String memberId) {
        try {
            authManager.validateMember(memberId);
            memberService.getMember(memberId); //Validate existence
            List<Membership> memberships = membershipService.getMembershipsForMember(memberId);
            return ResponseEntity.ok(MembershipConverter.convertMany(memberships));
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member has not been found", e);
        }
    }

    /**
     * Rest endpoint to get all memberships.
     *
     * @return all memberships
     */
    @GetMapping("/getAllMemberships")
    public ResponseEntity<List<Membership>> getAllMemberships() {
        try {
            return ResponseEntity.ok(this.membershipService.getAll());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Rest endpoint to get all active memberships of an HOA
     *
     * @param hoaId id of HOA
     * @return List of all members
     */
    @GetMapping("/getAllMemberships/{hoaId}")
    public ResponseEntity<List<Membership>> getAllMemberships(@PathVariable Long hoaId) {
        try {
            return ResponseEntity.ok(this.membershipService.getActiveMembershipsByHoaId(hoaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * >>>>>>> Draft board election
     * Returns all active memberships for user.
     */
    @GetMapping("/getActiveMemberships/{memberId}")
    public ResponseEntity<List<Membership>> getActiveMemberships(@PathVariable String memberId) {
        try {
            authManager.validateMember(memberId);
            memberService.getMember(memberId); //Validate existence
            List<Membership> memberships = membershipService.getActiveMemberships(memberId);
            return ResponseEntity.ok(memberships);
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member does not exist", e);
        }
    }

    /**
     * Checks whether a user and HOA exist.
     */
    //TODO: Verify that HOA exists
    public void validateExistence(HoaModel model) {
        try {
            authManager.validateMember(model.getMemberId());
            memberService.getMember(model.getMemberId());
        } catch (IllegalAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HOA or member are not stored", e);
        }
    }

    /**
     * Endpoint for authentication.
     *
     * @param request The login model
     * @return JWT token if the login is successful
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponseModel> authenticate(@RequestBody AuthenticationRequestModel request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getMemberId(),
                            request.getPassword()));
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e);
        }

        final UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(request.getMemberId());
        final String jwtToken = jwtTokenGenerator.generateToken(userDetails);
        return ResponseEntity.ok(new AuthenticationResponseModel(jwtToken));
    }

    /**
     * Endpoint for demoting board members into regular members (effective clearing the board of the HOA)
     *
     * @param hoaId id of Hoa to consider
     * @param key   Semi-secure validation key to ensure that only other ms could have called this endpoint, not a user
     * @return Boolean to represent the success of the operation
     */
    @PostMapping("/resetBoard/{hoaId}")
    public ResponseEntity<Boolean> demote(@PathVariable long hoaId, @RequestBody String key) {
        if (!secretClearBoardKey.equals(key)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Unauthorized to clear board");
        ResponseEntity<List<Membership>> memberships = this.getAllMemberships(hoaId);
        if (memberships.getBody() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Given hoa either does not exist, or no members are in it");
        memberships.getBody().stream().filter(Membership::isInBoard).forEach(m -> {
            try {
                membershipService.changeBoard(m, false);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
        });
        return ResponseEntity.ok(true);
    }

    /**
     * Endpoint for promoting a regular member into a board member
     * Always called after clearing a board, hence no check if the member is already a board member
     *
     * @param hoaId id of Hoa to consider
     * @param mem   list of all members (by memberId) that should be promoted
     * @param key   Semi-secure validation key to ensure that only other ms could have called this endpoint, not a user
     * @return Boolean to represent the success of the operation
     */
    @PostMapping("/promoteWinners/{hoaId}/{key}")
    public ResponseEntity<Boolean> promote(@PathVariable long hoaId, @RequestBody List<String> mem,
                                           @PathVariable String key) {
        if (!secretPromotionKey.equals(key)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Unauthorized to clear board");
        ResponseEntity<List<Membership>> memberships = this.getAllMemberships(hoaId);
        if (memberships.getBody() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Given hoa either does not exist, or no members are in it");
        memberships.getBody().stream().filter(m -> mem.contains(m.getMemberId())).forEach(m -> {
            try {
                membershipService.changeBoard(m, true);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
        });
        return ResponseEntity.ok(true);
    }

    /** Setter method used when AuthManager needs to be mocked
     * @param a - AuthManager to be mocked
     */
    public void setAuthenticationManager(AuthManager a) {
        this.authManager = a;
    }

    /** Setter method used when MemberService needs to be mocked
     * @param m - MemberService to be mocked
     */
    public void setMemberService(MemberService m) {
        this.memberService = m;
    }

    /** Setter method used when MembershipService needs to be mocked
     * @param m - MembershipService to be mocked
     */
    public void setMembershipService(MembershipService m) {
        this.membershipService = m;
    }

    /** Setter method used when HoaService needs to be mocked
     * @param h - HoaService to be mocked
     */
    public void setHoaService(HoaService h) {
        this.hoaService = h;
    }
}
