package com.example.todo.todoapi.service;


import com.example.todo.todoapi.dto.request.TodoCreateRequestDTO;
import com.example.todo.todoapi.dto.request.TodoModifyRequestDTO;
import com.example.todo.todoapi.dto.response.TodoDetailResponseDTO;
import com.example.todo.todoapi.dto.response.TodoListResponseDTO;
import com.example.todo.todoapi.entity.Todo;
import com.example.todo.todoapi.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;


    public TodoListResponseDTO create(final TodoCreateRequestDTO requestDTO) throws RuntimeException {
    //매개변수에 final ->전달받은 값을 서비스에서 바꿀 수 없게 만들어줌.
         todoRepository.save(requestDTO.toEntity());
        log.info("할일 저장 완료. 제목 : {}", requestDTO.getTitle());

        return retrieve();
    }






    public TodoListResponseDTO retrieve() { //글 목록 받아오는 코드를 메서드로 추출 ->메서드 호출하면 글 목록 받아올 수 있다.
        List<Todo> entityList = todoRepository.findAll(); //전체 목록 조회

        List<TodoDetailResponseDTO> dtoList = entityList.stream()
                .map(TodoDetailResponseDTO::new)
                .collect(Collectors.toList()); //글 목록 받아옴

        return TodoListResponseDTO.builder()
                .todos(dtoList)
                .build();
    }


    public TodoListResponseDTO delete(final String todoId) {
        try {
            todoRepository.deleteById(todoId);
        } catch (Exception e) {
            log.error("ID가 존재하지 않아 삭제에 실패했습니다.- ID :{}, ERR:{}" ,todoId, e.getMessage());
            throw new RuntimeException("ID가 존재하지 않아 삭제에 실패했습니다."); //서비스에서 오류 잡으면 컨트롤러에게도 알려줘야 하기 때문에
        }
        return retrieve();
    }

    public TodoListResponseDTO update(final TodoModifyRequestDTO requestDTO)
    throws RuntimeException {
        Optional<Todo> targetEntity = todoRepository.findById(requestDTO.getId());

        targetEntity.ifPresent(todo-> {
            todo.setDone(requestDTO.isDone()); //화면단에서 반전시켜 보낸 값을 ToDO 엔터티 done에 세팅해준다.
            //물론 백엔드에서 done을 반전시켜 화면단으로 내보내는 것도 가능하다.

            todoRepository.save(todo);
        //여기서 targetEntity는 왜 못넣나????
            //-> optional 타입이기 때문에 !!! 그래서 깐 데이터 todo를 주면 된다.
        });

        return retrieve();

    }
}
