package com.gasagency.config;

import com.gasagency.dto.ExpenseCategoryDTO;
import com.gasagency.dto.ExpenseDTO;
import com.gasagency.entity.ExpenseCategory;
import com.gasagency.entity.Expense;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // ExpenseCategory mapping
        mapper.addMappings(new PropertyMap<ExpenseCategory, ExpenseCategoryDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setName(source.getName());
                map().setDescription(source.getDescription());
                map().setIsActive(source.getIsActive());
                map().setCreatedDate(source.getCreatedDate());
                map().setCreatedBy(source.getCreatedBy());
                map().setUpdatedDate(source.getUpdatedDate());
                map().setUpdatedBy(source.getUpdatedBy());
            }
        });

        // Expense mapping
        mapper.addMappings(new PropertyMap<Expense, ExpenseDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setDescription(source.getDescription());
                map().setAmount(source.getAmount());
                map().setExpenseDate(source.getExpenseDate());
                map().setNotes(source.getNotes());
                map().setPaymentMode(source.getPaymentMode());
                map().setCreatedDate(source.getCreatedDate());
                map().setCreatedBy(source.getCreatedBy());
                map().setUpdatedDate(source.getUpdatedDate());
                map().setUpdatedBy(source.getUpdatedBy());
                // Skip bankAccountId and bankAccountName - these are manually mapped in service
                skip().setBankAccountId(null);
                skip().setBankAccountName(null);
            }
        });

        return mapper;
    }
}
