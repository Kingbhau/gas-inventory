package com.gasagency.service;

import com.gasagency.dto.ExpenseCategoryDTO;
import com.gasagency.entity.ExpenseCategory;
import com.gasagency.repository.ExpenseCategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository repository;
    private final ModelMapper modelMapper;

    public ExpenseCategoryService(ExpenseCategoryRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    public Page<ExpenseCategoryDTO> getAllCategories(Pageable pageable) {
        return repository.findAll(pageable)
                .map(category -> modelMapper.map(category, ExpenseCategoryDTO.class));
    }

    public List<ExpenseCategoryDTO> getActiveCategories() {
        return repository.findByIsActiveTrue()
                .stream()
                .map(category -> modelMapper.map(category, ExpenseCategoryDTO.class))
                .collect(Collectors.toList());
    }

    public List<String> getActiveNames() {
        return repository.findActiveNames();
    }

    public ExpenseCategoryDTO getCategoryById(Long id) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return modelMapper.map(category, ExpenseCategoryDTO.class);
    }

    public ExpenseCategoryDTO createCategory(ExpenseCategoryDTO dto) {
        // Check if category already exists
        if (repository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Category with name '" + dto.getName() + "' already exists");
        }

        ExpenseCategory category = modelMapper.map(dto, ExpenseCategory.class);
        category.setIsActive(true);

        ExpenseCategory saved = repository.save(category);
        return modelMapper.map(saved, ExpenseCategoryDTO.class);
    }

    public ExpenseCategoryDTO updateCategory(Long id, ExpenseCategoryDTO dto) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // Check if new name is already taken by another category
        if (!category.getName().equals(dto.getName()) &&
                repository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Category with name '" + dto.getName() + "' already exists");
        }

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        ExpenseCategory updated = repository.save(category);
        return modelMapper.map(updated, ExpenseCategoryDTO.class);
    }

    public void deleteCategory(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        repository.deleteById(id);
    }

    public ExpenseCategoryDTO toggleCategoryStatus(Long id, Boolean isActive) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        category.setIsActive(isActive);

        ExpenseCategory updated = repository.save(category);
        return modelMapper.map(updated, ExpenseCategoryDTO.class);
    }
}
