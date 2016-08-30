package fr.ippon.tatami.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.UserRepository;
import fr.ippon.tatami.security.SecurityUtils;
import fr.ippon.tatami.service.FriendshipService;
import fr.ippon.tatami.service.UserService;
import fr.ippon.tatami.service.util.DomainUtil;
import fr.ippon.tatami.web.rest.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Optional;

/**
 * REST controller for managing friendships.
 *
 * @author Julien Dubois
 */
@Controller
@RequestMapping("/tatami")
public class FriendshipResource {

    private final Logger log = LoggerFactory.getLogger(UserResource.class);

    @Inject
    private FriendshipService friendshipService;

    @Inject
    private UserService userService;

    @Inject
    private UserRepository userRepository;

    @RequestMapping(value = "/rest/users/{username}/friends",
        method = RequestMethod.GET,
        produces = "application/json")
    @Timed
    @ResponseBody
    public Collection<UserDTO> getFriends(@PathVariable("username") String username, HttpServletResponse response) {
        /*
         * Passing emails isn't safe, so instead of sometimes passing users and sometimes passing emails,
         * we're going to always pass usernames and then convert them to emails
         */
        String email = username + "@" + SecurityUtils.getCurrentUserDomain();
        Optional<User> optionalUser = userRepository.findOneByEmail(email);
        if (!optionalUser.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        User user = optionalUser.get();
        Collection<User> friends = friendshipService.getFriendsForUser(user.getEmail());

        return userService.buildUserDTOList(friends);
    }

    @RequestMapping(value = "/rest/users/{username}/followers",
        method = RequestMethod.GET,
        produces = "application/json")
    @Timed
    @ResponseBody
    public Collection<UserDTO> getFollowers(@PathVariable("username") String username, HttpServletResponse response) {
        /*
         * Passing emails isn't safe, so instead of sometimes passing users and sometimes passing emails,
         * we're going to always pass usernames and then convert them to emails
         */
        String email = username + "@" + SecurityUtils.getCurrentUserDomain();

        Optional<User> optionalUser = userRepository.findOneByEmail(email);
        if (!optionalUser.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        User user = optionalUser.get();
        Collection<User> friends = friendshipService.getFollowersForUser(user.getEmail());

        return userService.buildUserDTOList(friends);
    }

    /**
     * Added an "action" parameter to specify which type of PATCH we should do (Activate / Follow ).
     */

    @RequestMapping(value = "/rest/users/{username}",
        method = RequestMethod.PATCH)
    @Timed
    @ResponseBody
    public UserDTO updateFriend(@PathVariable("username") String username, HttpServletResponse response) {
        /*
         * Passing emails isn't safe, so instead of sometimes passing users and sometimes passing emails,
         * we're going to always pass usernames and then convert them to emails
         */
        String email = username + "@" + SecurityUtils.getCurrentUserDomain();
        Optional<User> optionalUser = userRepository.findOneByEmail(email);
        if (!optionalUser.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            log.warn("User {} doen't exist", email);
            return null;
        }
        UserDTO toReturn = userService.buildUserDTO(optionalUser.get());
        if (!toReturn.isFriend()) {
            friendshipService.followUser(toReturn.getEmail());
        } else {
            friendshipService.unfollowUser(toReturn.getEmail());
        }
        return toReturn;
    }

}