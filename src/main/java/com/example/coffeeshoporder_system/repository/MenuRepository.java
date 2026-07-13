package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.Menu;
import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByStatusOrderByIdAsc(MenuStatus status);

    List<Menu> findAllByIdInAndStatus(Collection<Long> ids, MenuStatus status);

    Optional<Menu> findByIdAndStatus(Long id, MenuStatus status);
}
