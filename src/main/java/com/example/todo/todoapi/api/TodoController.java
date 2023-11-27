package com.example.todo.todoapi.api;

import com.example.todo.todoapi.dto.request.TodoCreateRequestDTO;
import com.example.todo.todoapi.dto.request.TodoModifyRequestDTO;
import com.example.todo.todoapi.dto.response.TodoListResponseDTO;
import com.example.todo.todoapi.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/todos")
@CrossOrigin
public class TodoController {
    private final TodoService todoService;

    // 할 일 등록 요청
    @PostMapping
    public ResponseEntity<?> createTodo(
            @Validated @RequestBody TodoCreateRequestDTO requestDTO,
            BindingResult result
    ) {
        log.info("/api/todos POST");
        if (result.hasErrors()) {
            log.warn("DTO 검증 에러 발생: {}", result.getFieldError());
            return ResponseEntity.badRequest()
                    .body(result.getFieldError());
        }

        try {
            TodoListResponseDTO responseDTO = todoService.create(requestDTO);
            return ResponseEntity.ok()
                    .body(responseDTO);
        } catch (RuntimeException e) {
            log.error("error createTodo", e);
            return ResponseEntity.internalServerError()
                    .body(TodoListResponseDTO.builder()
                            .error(e.toString())
                            .build());
        }

    }

    //할일 목록 요청
    @GetMapping
    public ResponseEntity<?> retrieveTodoList() {
        log.info("/api/todos Get request");
       TodoListResponseDTO responseDTO =  todoService.retrieve();

       return ResponseEntity.ok().body(responseDTO);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(
            @PathVariable("id") String todoId
    ) { //변수명 아이디랑 똑같으면 pathvariable 사용 x
        log.info("/api/todos/{} DELETE request!", todoId);
        if(todoId ==null || todoId.trim().equals("")) {
            return ResponseEntity.badRequest().body(TodoListResponseDTO
                    .builder()
                    .error("Id를 전달해주세요")
                    .build());
        }

        try {
            TodoListResponseDTO responseDTO =  todoService.delete(todoId);
        return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(TodoListResponseDTO.builder().error(e.getMessage()).build());
            //여기서 responseDTO를 부르지 못한다 -> try문에서 responseDTO를 담았기 때문에

        }
    }

    //할일 수정하기( //할일 완료인지 아닌지를 체크하는 메서드이다.)
    @RequestMapping(method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<?> updateTodo(
            @Validated @RequestBody TodoModifyRequestDTO requestDTO,
            BindingResult result,
            HttpServletRequest request
    ) {

        if(result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getFieldError());
        }
        log.info("/api/todos {} request", request.getMethod());
        log.info("modifying dto : {}", requestDTO);

        try {
            TodoListResponseDTO responseDTO = todoService.update(requestDTO);
            return ResponseEntity.ok().body(responseDTO);
        } catch (RuntimeException e) {
         return ResponseEntity.internalServerError().body(TodoListResponseDTO.builder().error("존재하지 않는 ID라 수정 안됨!").build());
        }
    }


}
