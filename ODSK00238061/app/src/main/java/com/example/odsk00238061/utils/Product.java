package com.example.odsk00238061.utils;

import java.util.List;

public class Product {
    private String title;
    private String description;
    private String category;
    private String ingredients;
    private String nutritionFacts;
    private List<String> images;

    public Product () {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if(category.toLowerCase().contains("food")){
            this.category = "Consumable";
        } else {
            this.category = category;
        }
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients =  ingredients;
    }

    public String getNutritionFacts() {
        return nutritionFacts;
    }

    public void setNutritionFacts(String nutritionFacts) {
        this.nutritionFacts =  nutritionFacts;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public boolean isFood(){
        return category.toLowerCase().contains("food");
    }

    public boolean isIngredientsAvailable() {
        return ingredients != null && !ingredients.isEmpty();
    }

    public boolean isNutritionFactsAvailable() {
        return nutritionFacts != null && !nutritionFacts.isEmpty();
    }

    public boolean isTitleAvailable() {
        return title != null && !title.isEmpty();
    }

    public boolean isDescriptionAvailable() {
        return description != null && !description.isEmpty();
    }
}

