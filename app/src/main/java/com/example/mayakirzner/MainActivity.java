
package com.example.mayakirzner;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.widget.EditText;
import com.example.mayakirzner.servises.SignalRService;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mayakirzner.R;
import com.example.mayakirzner.models.TicTacToeModel;

public class MainActivity extends AppCompatActivity {
    private TicTacToeModel model;
    private final SignalRService signalRService = new SignalRService();
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        model = new TicTacToeModel();
    }
    public void onCellClick(View view) {
              Button button = (Button) view;
              String tag = button.getTag().toString();
              String[] position = tag.split(",");
              int row = Integer.parseInt(position[0]);
              int col = Integer.parseInt(position[1]);

              if (model.isLegal(row, col)) {
                  model.makeMove(row, col);
                  String player = model.getCurrentPlayer();
                  button.setText(player);
                  // Send the move to the hub: "row,col,player"
                  signalRService.sendMove(row, col, player);
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
                    button.setText("");
                }
            }
    public void onConnectClick(View view) {
        EditText ipEdit = findViewById(R.id.editServerIP);
        String ip = ipEdit.getText().toString().trim();
        if (ip.isEmpty()) {
            // Fall back to hint if empty
            ip = ipEdit.getHint() != null ? ipEdit.getHint().toString() : "109.226.44.197";
        }

        String finalIp = ip;
        Toast.makeText(this, "Connecting to " + finalIp + ":80", Toast.LENGTH_SHORT).show();
        signalRService.connect(finalIp, new SignalRService.Listener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connected to hub", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDisconnected(Throwable error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Hub error: " + (error != null ? error.getMessage() : "unknown"), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onReceiveMessage(String user, String message) {
                Log.d(TAG, "we got " + message + " from " + user);
            }

            @Override
            public void onReceiveKey(String key) {
                // בהמשך נטפל כאן בקבלת מידע מהשרת
                Log.d(TAG, "HandleOthersKey: " + key);
                // Expecting format: "row,col,player"
                String[] parts = key.split(",");
                if (parts.length != 3) {
                    return;
                }
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int c = Integer.parseInt(parts[1].trim());
                    String p = parts[2].trim();

                    runOnUiThread(() -> {
                        // Only apply if the cell is still empty
                        if (model.isLegal(r, c)) {
                            boolean applied = model.setMove(r, c, p);
                            if (applied) {
                                int id = idFor(r, c);
                                if (id != 0) {
                                    Button target = findViewById(id);
                                    if (target != null) {
                                        target.setText(p);
                                    }
                                    // Optional: win/tie checks would go here if implemented

                                    // Keep turn alternation consistent with local logic
                                }
                                model.changePlayer();
                            }
                        }
                    });
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Bad key format: " + key);
                }
            }
        });
    }



    private int idFor(int row, int col) {
        if (row == 0 && col == 0) return R.id.button00;
        if (row == 0 && col == 1) return R.id.button01;
        if (row == 0 && col == 2) return R.id.button02;
        if (row == 1 && col == 0) return R.id.button10;
        if (row == 1 && col == 1) return R.id.button11;
        if (row == 1 && col == 2) return R.id.button12;
        if (row == 2 && col == 0) return R.id.button20;
        if (row == 2 && col == 1) return R.id.button21;
        if (row == 2 && col == 2) return R.id.button22;
        return 0;
    }

}