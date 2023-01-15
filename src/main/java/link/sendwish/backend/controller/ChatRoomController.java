package link.sendwish.backend.controller;

import io.swagger.models.Model;
import link.sendwish.backend.dtos.*;
import link.sendwish.backend.dtos.chat.ChatRoomRequestDto;
import link.sendwish.backend.dtos.chat.ChatRoomResponseDto;
import link.sendwish.backend.entity.Member;
import link.sendwish.backend.service.ChatService;
import link.sendwish.backend.service.MemberService;
import link.sendwish.backend.service.SseEmitters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatRoomController {
    private final ChatService chatService;
    private final MemberService memberService;
    private final SseEmitters sseEmitters;

    // 채팅 리스트 화면
    @GetMapping("/room")
    public String rooms(Model model) {
        return "/chat/room";
    }


    // 모든 채팅방 목록 조회
    @GetMapping("/rooms/{nickname}")
    public ResponseEntity<?> getRoomsByMember(@PathVariable("nickname") String nickname) {
        try{
            Member member = memberService.findMember(nickname);

            List<ChatRoomResponseDto> chatRooms = chatService.findRoomByMember(member);

            return ResponseEntity.ok().body(chatRooms);
        }catch (Exception e) {
            e.printStackTrace();
            ResponseErrorDto errorDto = ResponseErrorDto.builder()
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorDto);
        }
    }

    // 채팅방 생성
    @PostMapping("/room")
    public ResponseEntity<?> createRoom(@RequestBody ChatRoomRequestDto dto) {
        try{
            ChatRoomResponseDto savedRoom = chatService.createRoom(dto.getMemberIdList(), dto.getCollectionId());
            return ResponseEntity.ok().body(savedRoom);
        }catch (Exception e) {
            e.printStackTrace();
            ResponseErrorDto errorDto = ResponseErrorDto.builder()
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorDto);
        }
    }

    // 채팅방 입장
    @GetMapping("/room/enter/{roomId}")
    public ResponseEntity<?> roomDetail(@PathVariable("roomId") Long roomId) {
        try{
            ChatRoomResponseDto room = chatService.findRoomById(roomId);
            return ResponseEntity.ok().body(room);
        }catch (Exception e) {
            e.printStackTrace();
            ResponseErrorDto errorDto = ResponseErrorDto.builder()
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorDto);
        }
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(SseEmitters sseEmitters) {
        SseEmitter emitter = new SseEmitter(60 * 1000L); // 1분 만료시간, default 30초
        sseEmitters.add(emitter); // 향후 이벤트 발생시, 해당 클라이언트로 이벤트 전송 위해 저장
        try {
            emitter.send(SseEmitter.event()
                    .name("connected") // 해당 이벤트 이름
                    .data("connected!!!")); // 503 에러 방지 더미 데이터
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body(emitter);
    }

    @PostMapping("/count") // 서버에 변경사항 있을 때, 클라이언트 요청 없이 데이터 전송
    public ResponseEntity<?> count(){
        sseEmitters.count();
        return ResponseEntity.ok().build();
    }
}

