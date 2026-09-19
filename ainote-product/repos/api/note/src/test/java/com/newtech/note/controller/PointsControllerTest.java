package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.UserLevelEnum;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.entity.dto.PointsDataEnum;
import com.newtech.note.entity.dto.UpdatePointsReq;
import com.newtech.note.entity.dto.UserInfoDetail;
import com.newtech.note.service.PointsService;
import com.newtech.note.service.UserService;
import com.newtech.note.security.RequestIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PointsControllerTest {

    @Test
    void guestPointsResponseDoesNotCallPersistenceServices() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        MockServerHttpRequest request = guestRequest();
        PointsController controller = new PointsController(pointsService, userService, guestIdentity(request));

        UserInfoDetail response = controller.getAvailablePoints(request).block();

        assertNotNull(response);
        assertEquals(1_000_000.0, response.getPoints());
        assertTrue(response.getPointsHistory().isEmpty());
        assertEquals(UserLevelEnum.HUMAN, response.getLevel());
        verifyNoInteractions(pointsService, userService);
    }

    @Test
    void guestPointsUpdateIsGoneAndDoesNotReachPersistence() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        MockServerHttpRequest request = guestRequest();
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        PointsController controller = new PointsController(pointsService, userService, identityService);

        ResponseEntity<NoteBaseResponse<Void>> response = controller.updatePoints(
                new UpdatePointsReq(PointsChangeEnum.ADD, 1000.0, PointsDataEnum.YEAR), request).block();

        assertNotNull(response);
        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.GONE.value(), response.getBody().getCode());
        verifyNoInteractions(pointsService, userService, identityService);
    }

    @Test
    void mobileUserCannotAddClientSelectedPoints() {
        PointsService pointsService = mock(PointsService.class);
        UserService userService = mock(UserService.class);
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        MockServerHttpRequest request = MockServerHttpRequest.post("/points/update")
                .header("Authorization", "Mobilejwt-token")
                .build();
        PointsController controller = new PointsController(pointsService, userService, identityService);

        ResponseEntity<NoteBaseResponse<Void>> response = controller.updatePoints(
                new UpdatePointsReq(PointsChangeEnum.ADD, 1000000.0, PointsDataEnum.YEAR), request).block();

        assertNotNull(response);
        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.GONE.value(), response.getBody().getCode());
        verifyNoInteractions(pointsService, userService, identityService);
    }

    private MockServerHttpRequest guestRequest() {
        return MockServerHttpRequest.get("/points/get")
                .header("Authorization", "Guest test-device")
                .header("Device-Id", "test-device")
                .build();
    }

    private RequestIdentityService guestIdentity(MockServerHttpRequest request) {
        RequestIdentityService identityService = mock(RequestIdentityService.class);
        when(identityService.resolve(request)).thenReturn(Mono.just(
                new RequestIdentityService.RequestIdentity("test-device", null, null, null, true)));
        return identityService;
    }
}
