package link.sendwish.backend.controller;

import link.sendwish.backend.common.exception.DtoNullException;
import link.sendwish.backend.dtos.ResponseErrorDto;
import link.sendwish.backend.dtos.chat.ChatVoteRequestDto;
import link.sendwish.backend.dtos.chat.ChatVoteResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(produces = "application/json;charset=UTF-8")
public class VoteController {
    private final RedisTemplate<String, String> redisTemplate;

    // like 추가
    @PostMapping("/like")
    public ResponseEntity<?> like(@RequestBody ChatVoteRequestDto dto) {
        try {
            if (dto.getRoomId() == null || dto.getNickname() == null || dto.getItemId() == null) {
                throw new DtoNullException();
            }
            final ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            final String key = dto.getRoomId() + ":" + dto.getItemId();
            final String value = dto.getNickname();

            final String find = valueOperations.get(key);
            if (find == null) {
                valueOperations.set(key, value);
            } else {
                if(!find.contains(value)) {
                    valueOperations.set(key, find + "," + value);
                }else {
                    String res = find.replace("," + value, "");
                    System.out.println("replace " + res);
                    valueOperations.set(key, res);
                }
            }

            final String result = valueOperations.get(key);
            System.out.println("result = " + result);

            Integer count = result.length() - result.replace(",", "").length() + 1;
            System.out.println("count = " + count);

            ChatVoteResponseDto responseDto = ChatVoteResponseDto.builder()
                    .itemId(dto.getItemId())
                    .like(count)
                    .build();
            return ResponseEntity.ok().body(responseDto);
        }catch (Exception e) {
            e.printStackTrace();
            ResponseErrorDto errorDto = ResponseErrorDto.builder()
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorDto);
        }
    }
}
