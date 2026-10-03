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
 * Экран RTDB лобби: публикация комнат и их отображение в реальном времени.
 */
public class Main2Activity extends AppCompatActivity {

    private TicTacToeModel model;
    private ActivityMain2Binding binding;
    private DatabaseReference gamesReference;
    private ValueEventListener gamesListener;

    private final List<String> roomLabels = new ArrayList<>();
    private ArrayAdapter<String> roomsAdapter;
    private String currentUid;

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

        setupRoomSpinner();
        binding.buttonStartGame.setOnClickListener(view -> startGameAndWait());
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
    }

    private void listenForRooms() {
        gamesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomLabels.clear();

                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    GameRoom room = roomSnapshot.getValue(GameRoom.class);
                    if (room != null) {
                        String state = room.getPlayerO().isEmpty()
                                ? getString(R.string.rtdb_room_waiting)
                                : getString(R.string.rtdb_room_playing);
                        roomLabels.add(getString(R.string.rtdb_room_label, room.getName(), state));
                    }
                }

                roomsAdapter.notifyDataSetChanged();
                binding.textNoGames.setVisibility(
                        roomLabels.isEmpty() ? View.VISIBLE : View.GONE
                );
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
                .addOnSuccessListener(unused -> {
                    binding.editGameName.setText("");
                    Toast.makeText(this, R.string.rtdb_game_published, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> Toast.makeText(
                        this,
                        getString(R.string.rtdb_write_failed, error.getMessage()),
                        Toast.LENGTH_LONG
                ).show());
    }

    public void onCellClick(View view) {
        Button button = (Button) view;
        String tag = button.getTag().toString();
        String[] position = tag.split(",");
        int row = Integer.parseInt(position[0]);
        int col = Integer.parseInt(position[1]);

        if (model.isLegal(row, col)) {
            model.makeMove(row, col);
            button.setText(model.getCurrentPlayer());

            if (model.checkWin()) {
                model.changePlayer();
                Toast.makeText(this, "Player " + model.getCurrentPlayer() + " wins!", Toast.LENGTH_SHORT).show();
                model.resetGame();
                resetBoard();
            } else if (model.isTie()) {
                Toast.makeText(this, "It's a tie!", Toast.LENGTH_SHORT).show();
                model.resetGame();
                resetBoard();
            } else {
                model.changePlayer();
            }
        }
    }

    private void resetBoard() {
        int[] buttonIds = {
                R.id.button00, R.id.button01, R.id.button02,
                R.id.button10, R.id.button11, R.id.button12,
                R.id.button20, R.id.button21, R.id.button22
        };

        for (int id : buttonIds) {
            Button button = findViewById(id);
            if (button != null) {
                button.setText("");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (gamesListener != null && gamesReference != null) {
            gamesReference.removeEventListener(gamesListener);
        }
        super.onDestroy();
    }
}