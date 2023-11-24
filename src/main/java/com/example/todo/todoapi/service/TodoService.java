package com.example.todo.todoapi.service;


import com.example.todo.todoapi.dto.request.TodoCreateRequestDTO;
import com.example.todo.todoapi.dto.response.TodoDetailResponseDTO;
import com.example.todo.todoapi.dto.response.TodoListResponseDTO;
import com.example.todo.todoapi.entity.Todo;
import com.example.todo.todoapi.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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


}
