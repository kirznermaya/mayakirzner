package com.example.mayakirzner.models;

/**
 * Модель данных для одной игровой комнаты в Firebase RTDB.
 */
public class GameRoom {

    private String name = "";
    private String playerX = "";
    private String playerO = "";
    private String moves = "";

    /**
     * Обязательный пустой конструктор для Firebase!
     */
    public GameRoom() {
    }

    /**
     * Конструктор для создания новой комнаты в коде.
     */
    public GameRoom(String name, String playerX, String playerO, String moves) {
        this.name = name;
        this.playerX = playerX;
        this.playerO = playerO;
        this.moves = moves;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlayerX() {
        return playerX;
    }

    public void setPlayerX(String playerX) {
        this.playerX = playerX;
    }

    public String getPlayerO() {
        return playerO;
    }

    public void setPlayerO(String playerO) {
        this.playerO = playerO;
    }

    public String getMoves() {
        return moves;
    }

    public void setMoves(String moves) {
        this.moves = moves;
    }
}