package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.plaf.DimensionUIResource; //! Switch to "Dimesion" if ui not responding faster
import pieces.Bishop;
import pieces.King;
import pieces.Knight;
import pieces.Pawn;
import pieces.Piece;
import pieces.Queen;
import pieces.Rook;

public class GamePanel extends JPanel implements Runnable {

    public static final int WIDTH = 1100;
    public static final int HEIGHT = 800;

    final int FPS = 60;

    // *implement thread
    Thread gameThread;

    Board board = new Board();
    Mouse mouse = new Mouse();

    //?Piece
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promoPieces = new ArrayList<>();
    Piece activeP;
    public static Piece castlingP;
    //?color 
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;
    //?Booleans
    boolean canMove;
    boolean validSquare;
    boolean promotion;

    public GamePanel() {
        setPreferredSize(new DimensionUIResource(WIDTH, HEIGHT));
        setBackground(Color.GRAY);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces() {
        //?White
        //pawn
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));
        //knight
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Knight(WHITE, 6, 7));

        //rook
        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Rook(WHITE, 7, 7));

        //bishop
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        //queen
        pieces.add(new Queen(WHITE, 3, 7));
        //king
        pieces.add(new King(WHITE, 4, 7));

        //?Black
        //pawn
        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));
        //knight
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Knight(BLACK, 6, 0));

        //rook
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 7, 0));
        //bishop
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        //queen
        pieces.add(new Queen(BLACK, 3, 0));
        //king
        pieces.add(new King(BLACK, 4, 0));

    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for (int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    @Override
    public void run() {
        //* GameLoop
        // Calculate the interval between frames in nanoseconds
        // FPS stands for "Frames Per Second". This controls how fast the game should
        // run.
        double drawInterval = 1000000000 / FPS;// 1 second = 1 billion nanoseconds
        double delta = 0;// Will store the time difference between frames

        // Get the current time in nanoseconds to track frame timing
        long lastTime = System.nanoTime();
        long currentTime;

        // Main game loop, which continues to run while the game thread is active
        while (gameThread != null) {
            // Get the current time in nanoseconds
            currentTime = System.nanoTime();

            // Calculate the time passed since the last frame and normalize by the draw interval
            // This helps track how much of a frame has passed, even if it's less than a full frame
            delta += (currentTime - lastTime) / drawInterval;
            // Update the last time marker to the current time for the next loop iteration
            lastTime = currentTime;

            // If enough time has passed to display a new frame (i.e., delta >= 1)
            if (delta >= 1) {
                // Call the update method to update game logic
                update();
                // Call the repaint method to refresh the screen and render the updated visuals
                repaint();
                // Decrease delta by 1 to account for the processed frame
                delta--;
            }
        }

    }

    // !the above method calls the update and paintComponent 60 times per second
    private void update() {
        if (promotion) {
            promoting();
        } else {

            //!Mouse pressed
            if (mouse.pressed) {
                if (activeP == null) {
                    //?if activeP is null, check if  u can pick up a piece
                    for (Piece piece : simPieces) {
                        //?if mouse on ally piece check if u can pick up it as a activeP
                        if (piece.color == currentColor
                                && piece.col == mouse.x / Board.SQUARE_SIZE
                                && piece.row == mouse.y / Board.SQUARE_SIZE) {
                            activeP = piece;
                        }
                    }
                } else {
                    //!if palyer is holding a piece
                    simulate();
                }
            }
            //!Mouse Released
            if (mouse.pressed == false) {
                if (activeP != null) {
                    if (validSquare) {
                        //*Move Confirmed */

                        //*Update the piece list in case if the piece has been captured and removed during the simulation */
                        copyPieces(simPieces, pieces);
                        activeP.updatePosition();

                        //?castling move mouse update
                        if (castlingP != null) {
                            castlingP.updatePosition();
                        }
                        if (canPromote()) {
                            promotion = true;
                        } else {
                            changePlayer();
                        }

                    } else {
                        //* The Move was not valid so restores using the backup list Pieces */
                        copyPieces(pieces, simPieces);
                        activeP.resetPosition();
                        activeP = null;
                    }
                }
            }
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;

        //*Reset the piece list in every loop */
        //*Restores the removed piece during the simulation */
        copyPieces(pieces, simPieces);

        //?reset castling ROOK position
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }
        //?if piece is held by user , position is "updated"
        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        //?check if the piece is hovering over a valid "tile"
        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;
            //!stimuation
            //?if hitting an opponent piece ,remove from list
            if (activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }
            checkcastling();
            validSquare = true;
        }
    }

    private void checkcastling() {
        if (castlingP != null) {
            if (castlingP.col == 0) {
                castlingP.col += 3;
            } else if (castlingP.col == 7) {
                castlingP.col -= 2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void changePlayer() {
        if (currentColor == WHITE) {
            currentColor = BLACK;
            //?en passant 
            //* reset the black two step status
            for (Piece piece : pieces) {
                if (piece.color == BLACK) {
                    piece.twoStepped = false;
                }
            }
        } else {
            currentColor = WHITE;
            //?en passant 
            //* reset the white two step status
            for (Piece piece : pieces) {
                if (piece.color == WHITE) {
                    piece.twoStepped = false;
                }
            }
        }
        activeP = null;
    }

    private boolean canPromote() {
        if (activeP.type == Type.PAWN) {
            if (currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
                promoPieces.clear();
                promoPieces.add(new Rook(currentColor, 9, 2));
                promoPieces.add(new Knight(currentColor, 9, 3));
                promoPieces.add(new Bishop(currentColor, 9, 4));
                promoPieces.add(new Queen(currentColor, 9, 5));
                return true;
            }
        }
        return false;
    }

    //? mouse action for status screen 
    private void promoting() {
        if (mouse.pressed) {
            for (Piece piece : promoPieces) {
                if (piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
                    switch (piece.type) {
                        case ROOK:
                            simPieces.add(new Rook(currentColor, activeP.col, activeP.row));
                            break;
                        case KNIGHT:
                            simPieces.add(new Knight(currentColor, activeP.col, activeP.row));
                            break;
                        case BISHOP:
                            simPieces.add(new Bishop(currentColor, activeP.col, activeP.row));
                            break;
                        case QUEEN:
                            simPieces.add(new Queen(currentColor, activeP.col, activeP.row));
                            break;
                        default:
                            break;
                    }
                    simPieces.remove(activeP.getIndex());
                    copyPieces(simPieces, pieces);
                    activeP=null;
                    promotion=false;
                    changePlayer();
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        //?board
        board.draw(g2);
        //?pieces
        for (Piece p : simPieces) {
            p.draw(g2);
        }
        if (activeP != null) {
            if (canMove) {
                g2.setColor(Color.WHITE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }

            //confirm if the piece on right tile and resetting it's opacity
            activeP.draw(g2);
        }
        //?Status msg
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Book Antiqua", Font.ITALIC, 35));
        g2.setColor(Color.WHITE);

        if (promotion) {
            g2.drawString("Promote to ", 840, 150);
            for (Piece piece : promoPieces) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row),
                        Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
            }
        } else {
            if (currentColor == WHITE) {
                g2.drawString("WHITE'S turn", 840, 550);
            } else {
                g2.drawString("BLACK'S turn", 840, 250);

            }
        }
    }
}
