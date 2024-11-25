package com.example.demo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Arrays;

public class TicTacToeServer {

    private static final int PORT = 8080;
    private static final char EMPTY = '-';
    private static char[][] board = new char[3][3];
    private static char currentPlayer = 'X';
    private static boolean gameRunning = true;

    public static void main(String[] args) {
        Arrays.stream(board).forEach(row -> Arrays.fill(row, EMPTY));

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(new StringDecoder(), new StringEncoder(), new ServerHandler());
                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind(PORT).sync();
            System.out.println("Server started on port " + PORT);
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class ServerHandler extends SimpleChannelInboundHandler<String> {
        private static int playerCount = 0;
        private static Channel player1;
        private static Channel player2;

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            playerCount++;
            if (playerCount == 1) {
                player1 = ctx.channel();
                player1.writeAndFlush("Welcome Player 1 (X)\n");
                System.out.println("Player 1 connected");
            } else if (playerCount == 2) {
                player2 = ctx.channel();
                player2.writeAndFlush("Welcome Player 2 (O)\n");
                System.out.println("Player 2 connected");
                startGame();
            } else {
                ctx.channel().writeAndFlush("Game is full!\n").addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            msg = msg.trim();
            System.out.println("Received message: " + msg);

            if (!gameRunning) {
                ctx.channel().writeAndFlush("Game over! Please disconnect.\n");
                return;
            }

            if (ctx.channel() == player1 && currentPlayer != 'X' || ctx.channel() == player2 && currentPlayer != 'O') {
                ctx.channel().writeAndFlush("Wait for your turn!\n");
                return;
            }

            try {
                String[] tokens = msg.split(" ");
                if (tokens.length != 2) {
                    throw new Exception("Invalid format");
                }

                int x = Integer.parseInt(tokens[0]);
                int y = Integer.parseInt(tokens[1]);

                if (x < 0 || x > 2 || y < 0 || y > 2 || board[x][y] != EMPTY) {
                    ctx.channel().writeAndFlush("Invalid move! Try again.\n");
                } else {
                    board[x][y] = currentPlayer;
                    broadcast("Player " + currentPlayer + " moved to (" + x + "," + y + ")\n");
                    broadcast(displayBoard());
                    if (checkWin(currentPlayer)) {
                        broadcast("Player " + currentPlayer + " wins!\n");
                        endGame();
                    } else if (isBoardFull()) {
                        broadcast("Game is a draw!\n");
                        endGame();
                    } else {
                        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                        broadcast("Now it's Player " + currentPlayer + "'s turn.\n");
                    }
                }
            } catch (Exception e) {
                ctx.channel().writeAndFlush("Invalid input! Use 'x y' format.\n");
            }
        }

        private void startGame() {
            broadcast("Game started! Player X goes first.\n");
            broadcast(displayBoard());
        }

        private void broadcast(String message) {
            System.out.println("Broadcasting message: " + message);
            if (player1 != null) player1.writeAndFlush(message);
            if (player2 != null) player2.writeAndFlush(message);
        }

        private String displayBoard() {
            StringBuilder boardString = new StringBuilder();
            boardString.append("Current Board State:\n");
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    boardString.append(board[i][j]).append(" ");
                }
                boardString.append("\n");
            }
            return boardString.toString();
        }

        private boolean checkWin(char player) {
            for (int i = 0; i < 3; i++) {
                if (board[i][0] == player && board[i][1] == player && board[i][2] == player) return true;
                if (board[0][i] == player && board[1][i] == player && board[2][i] == player) return true;
            }
            return (board[0][0] == player && board[1][1] == player && board[2][2] == player) ||
                    (board[0][2] == player && board[1][1] == player && board[2][0] == player);
        }

        private boolean isBoardFull() {
            for (char[] row : board) {
                for (char cell : row) {
                    if (cell == EMPTY) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void endGame() {
            gameRunning = false;
            broadcast("Game over! Disconnect players.....\n");

            if (player1 != null) {
                player1.writeAndFlush("Thank you for playing!\n").addListener(ChannelFutureListener.CLOSE);
            }
            if (player2 != null) {
                player2.writeAndFlush("Thank you for playing!\n").addListener(ChannelFutureListener.CLOSE);
            }

            player1 = null;
            player2 = null;
            playerCount = 0;
            Arrays.stream(board).forEach(row -> Arrays.fill(row, EMPTY));
            currentPlayer = 'X';
            gameRunning = true;
        }
    }
}
