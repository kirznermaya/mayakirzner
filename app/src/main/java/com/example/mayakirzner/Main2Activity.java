package com.example.mayakirzner;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mayakirzner.R;
import com.example.mayakirzner.databinding.ActivityMain2Binding;
import com.example.mayakirzner.models.GameRoom;
import com.example.mayakirzner.models.TicTacToeModel;
import com.example.mayakirzner.servises.FBRef;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Сетевая игра Tic-Tac-Toe с лобби и поддержкой зрителей через Firebase RTDB.
 */
public class Main2Activity extends AppCompatActivity {

    private TicTacToeModel model;
    private ActivityMain2Binding binding;

    private DatabaseReference gamesReference;
    private ValueEventListener gamesListener;

    private DatabaseReference selectedRoomReference;
    private ValueEventListener selectedRoomListener;

    private final List<GameRoom> rooms = new ArrayList<>();
    private final List<String> roomIds = new ArrayList<>();
    private final List<String> roomLabels = new ArrayList<>();

    private ArrayAdapter<String> roomsAdapter;
    private GameRoom selectedRoom;
    private String currentUid;
    private String localPlayer = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser currentUser = FBRef.refAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.rtdb_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUid = currentUser.getUid();
        gamesReference = FBRef.refGames;
        model = new TicTacToeModel();

        if (binding.toolbar != null) {
            binding.toolbar.setNavigationOnClickListener(v -> finish());
        }

        setupRoomSpinner();
        binding.buttonStartGame.setOnClickListener(view -> startGameAndWait());
        binding.buttonOpenGame.setOnClickListener(view -> openSelectedGame());
        listenForRooms();
    }

    private void setupRoomSpinner() {
        roomsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                roomLabels
        );
        roomsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGames.setAdapter(roomsAdapter);
        binding.buttonOpenGame.setEnabled(false);
    }

    private void listenForRooms() {
        gamesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rooms.clear();
                roomIds.clear();
                roomLabels.clear();

                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    GameRoom room = roomSnapshot.getValue(GameRoom.class);
                    if (room != null) {
                        rooms.add(room);
                        roomIds.add(roomSnapshot.getKey());

                        String state = room.getPlayerO().isEmpty()
                                ? getString(R.string.rtdb_room_waiting)
                                : getString(R.string.rtdb_room_playing);
                        roomLabels.add(getString(R.string.rtdb_room_label, room.getName(), state));
                    }
                }

                roomsAdapter.notifyDataSetChanged();
                boolean hasRooms = !rooms.isEmpty();
                binding.textNoGames.setVisibility(hasRooms ? View.GONE : View.VISIBLE);
                binding.buttonOpenGame.setEnabled(hasRooms);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        Main2Activity.this,
                        getString(R.string.rtdb_read_failed, error.getMessage()),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        gamesReference.addValueEventListener(gamesListener);
    }

    private void startGameAndWait() {
        String gameName = binding.editGameName.getText().toString().trim();
        if (gameName.isEmpty()) {
            binding.editGameName.setError(getString(R.string.rtdb_game_name_required));
            binding.editGameName.requestFocus();
            return;
        }

        DatabaseReference newRoomReference = gamesReference.push();
        GameRoom room = new GameRoom(gameName, currentUid, "", "");
        newRoomReference.setValue(room)
                .addOnSuccessListener(unused -> openRoom(newRoomReference.getKey()))
                .addOnFailureListener(error -> Toast.makeText(
                        this,
                        getString(R.string.rtdb_write_failed, error.getMessage()),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void openSelectedGame() {
        int position = binding.spinnerGames.getSelectedItemPosition();
        if (position < 0 || position >= rooms.size()) {
            return;
        }

        GameRoom room = rooms.get(position);
        String roomId = roomIds.get(position);

        if (currentUid.equals(room.getPlayerX()) || currentUid.equals(room.getPlayerO())) {
            openRoom(roomId);
        } else if (room.getPlayerO().isEmpty()) {
            gamesReference.child(roomId).child("playerO").setValue(currentUid)
                    .addOnSuccessListener(unused -> openRoom(roomId))
                    .addOnFailureListener(error -> Toast.makeText(
                            this,
                            getString(R.string.rtdb_write_failed, error.getMessage()),
                            Toast.LENGTH_LONG
                    ).show());
        } else {
            openRoom(roomId);
        }
    }

    private void openRoom(String roomId) {
        binding.lobbyLayout.setVisibility(View.GONE);
        binding.gameLayout.setVisibility(View.VISIBLE);
        setBoardEnabled(false);

        selectedRoomReference = gamesReference.child(roomId);
        selectedRoomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                selectedRoom = snapshot.getValue(GameRoom.class);
                if (selectedRoom != null) {
                    showSelectedRoom();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        Main2Activity.this,
                        getString(R.string.rtdb_read_failed, error.getMessage()),
                        Toast.LENGTH_LONG
                ).show();
            }
        };
        selectedRoomReference.addValueEventListener(selectedRoomListener);
    }

    private void showSelectedRoom() {
        binding.textGameName.setText(selectedRoom.getName());
        model.resetGame();
        resetBoard();

        String moves = selectedRoom.getMoves();
        if (!moves.isEmpty()) {
            String[] moveList = moves.split(";");
            for (String move : moveList) {
                String[] parts = move.split(",");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                String player = parts[2];

                model.setMove(row, col, player);
                Button btn = buttonFor(row, col);
                if (btn != null) {
                    btn.setText(player);
                }
                model.changePlayer();
            }
        }

        if (currentUid.equals(selectedRoom.getPlayerX())) {
            localPlayer = "X";
        } else if (currentUid.equals(selectedRoom.getPlayerO())) {
            localPlayer = "O";
        } else {
            localPlayer = "";
        }

        showGameStatus();
    }

    private void showGameStatus() {
        if (selectedRoom.getPlayerO().isEmpty()) {
            binding.textGameStatus.setText(R.string.rtdb_waiting_for_player);
            setBoardEnabled(false);
        } else if (localPlayer.isEmpty()) {
            binding.textGameStatus.setText(R.string.rtdb_watching_game);
            setBoardEnabled(false);
        } else if (localPlayer.equals(model.getCurrentPlayer())) {
            binding.textGameStatus.setText(getString(R.string.rtdb_your_turn, localPlayer));
            setBoardEnabled(true);
        } else {
            binding.textGameStatus.setText(getString(R.string.rtdb_other_turn, localPlayer));
            setBoardEnabled(false);
        }
    }

    public void onCellClick(View view) {
        if (selectedRoom == null
                || selectedRoom.getPlayerO().isEmpty()
                || localPlayer.isEmpty()
                || !localPlayer.equals(model.getCurrentPlayer())) {
            return;
        }

        Button button = (Button) view;
        String[] position = button.getTag().toString().split(",");
        int row = Integer.parseInt(position[0]);
        int col = Integer.parseInt(position[1]);

        if (!model.isLegal(row, col)) {
            return;
        }

        String move = row + "," + col + "," + localPlayer;
        String previousMoves = selectedRoom.getMoves();
        String updatedMoves = previousMoves.isEmpty() ? move : previousMoves + ";" + move;

        setBoardEnabled(false);
        selectedRoomReference.child("moves").setValue(updatedMoves)
                .addOnFailureListener(error -> {
                    showGameStatus();
                    Toast.makeText(
                            this,
                            getString(R.string.rtdb_write_failed, error.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private Button buttonFor(int row, int col) {
        if (row == 0 && col == 0) return binding.button00;
        if (row == 0 && col == 1) return binding.button01;
        if (row == 0 && col == 2) return binding.button02;
        if (row == 1 && col == 0) return binding.button10;
        if (row == 1 && col == 1) return binding.button11;
        if (row == 1 && col == 2) return binding.button12;
        if (row == 2 && col == 0) return binding.button20;
        if (row == 2 && col == 1) return binding.button21;
        if (row == 2 && col == 2) return binding.button22;
        return null;
    }

    private Button[] boardButtons() {
        return new Button[]{
                binding.button00, binding.button01, binding.button02,
                binding.button10, binding.button11, binding.button12,
                binding.button20, binding.button21, binding.button22
        };
    }

    private void resetBoard() {
        for (Button button : boardButtons()) {
            if (button != null) {
                button.setText("");
            }
        }
    }

    private void setBoardEnabled(boolean enabled) {
        for (Button button : boardButtons()) {
            if (button != null) {
                button.setEnabled(enabled);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (gamesListener != null && gamesReference != null) {
            gamesReference.removeEventListener(gamesListener);
        }
        if (selectedRoomListener != null && selectedRoomReference != null) {
            selectedRoomReference.removeEventListener(selectedRoomListener);
        }
        super.onDestroy();
    }
}