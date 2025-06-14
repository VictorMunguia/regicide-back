package com.panda.regicide.websocket;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panda.regicide.dtos.PaginationResponse;
import com.panda.regicide.dtos.RoomDTO;
import com.panda.regicide.models.*;
import com.panda.regicide.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SocketEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(SocketEventHandler.class);

    private final SocketIOServer server;

    // Jackson para (de)serializar si te hace falta en algún momento
    private final ObjectMapper mapper = new ObjectMapper();

    // Almacén en memoria de salas (similar a "let rooms = {}" en Node).
    private Map<String, Room> rooms = new ConcurrentHashMap<>();

    @Autowired
    public SocketEventHandler(SocketIOServer server) {
        this.server = server;

        // Listener de conexión
        this.server.addConnectListener(client -> {
            System.out.println("Usuario conectado: " + client.getSessionId());
        });

        // Listener de desconexión
        this.server.addDisconnectListener(client -> {
            System.out.println("Usuario desconectado: " + client.getSessionId());
            // Eliminar de las rooms
            rooms.forEach((roomName, room) -> {
                room.getPlayers().removeIf(p -> p.getId().equals(client.getSessionId().toString()));
                // Si la room queda vacía, se borra
                if (room.getPlayers().isEmpty()) {
                    rooms.remove(roomName);
                } else {
                    broadcastPlayers(roomName);
                }
            });
            broadcastRooms();
        });

        // Registramos los listeners para los eventos igual que en Node:
        this.server.addEventListener("createRoom", CreateRoomRequest.class, (client, createRoomRequest, ackRequest) -> {
            handleCreateRoom(client, createRoomRequest, ackRequest);
        });

        this.server.addEventListener("getRooms", PaginationRequest.class, (client, paginationRequest, ackRequest) -> {
            handleGetRooms(client, paginationRequest);
        });

        this.server.addEventListener("joinRoom", String.class, (client, roomName, ackRequest) -> {
            handleJoinRoom(client, roomName, ackRequest);
        });

        this.server.addEventListener("startGame", String.class, (client, roomName, ackRequest) -> {
            handleStartGame(client, roomName, ackRequest);
        });

        this.server.addEventListener("playTurn", PlayTurnData.class, (client, playTurnData, ackRequest) -> {
            handlePlayTurn(client, playTurnData, ackRequest);
        });
    }

    /* =========================================================================
       ========================== MANEJO DE EVENTOS ============================
       ========================================================================= */

    private void handleCreateRoom(SocketIOClient client, CreateRoomRequest createRoomRequest, AckRequest ack) {
        String roomName = createRoomRequest.getRoomName();
        System.out.println(">>> createRoom: " + roomName);

        if (rooms.containsKey(roomName)) {
            ack.sendAckData(Map.of("success", false, "message", "Esta sala ya existe: " + roomName));
            return;
        }

        Room newRoom = new Room(roomName);
        newRoom.setMaxPlayerNumber(5);//  by default for now, front end will send this info later
        rooms.put(roomName, newRoom);

        ack.sendAckData(Map.of("success", true, "message", "La sala ha sido creada: " + roomName));
        client.sendEvent("roomResponse", Map.of("success", true, "message", "Sala creada"));
        handleGetRooms(client, createRoomRequest);
    }

    private void handleGetRooms(SocketIOClient client, PaginationRequest paginationRequest) {
        int page = Integer.parseInt(paginationRequest.getPage());
        int size = Integer.parseInt(paginationRequest.getSize());

        List<String> roomNames = new ArrayList<>(rooms.keySet());
        int totalRooms = roomNames.size();
        int totalPages = (int) Math.ceil((double) totalRooms / size);

        page = Math.max(1, Math.min(page, totalPages));

        List<String> paginatedRoomNames = paginateList(roomNames, page, size);
        List<RoomDTO> roomsResponse = paginatedRoomNames.stream()
                .map(roomName -> {
                    Room room = rooms.get(roomName);
                    return new RoomDTO(room.getRoomName(), String.valueOf(room.getPlayers().size()),
                            String.valueOf(room.getMaxPlayerNumber()));
                })
                .collect(Collectors.toList());

        PaginationResponse response = new PaginationResponse(roomsResponse, String.valueOf(totalRooms),
                String.valueOf(totalPages), String.valueOf(page));
        logger.info("updateRooms response: " + JsonUtils.convertObjectToString(response));
        client.sendEvent("updateRooms", response);
    }

    private List<String> paginateList(List<String> list, int page, int size) {
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, list.size());
        return list.subList(fromIndex, toIndex);
    }

    private void handleJoinRoom(SocketIOClient client, String roomName, AckRequest ack) {
        System.out.println(">>> joinRoom: " + roomName);

        Room room = rooms.get(roomName);
        if (room == null) {
            client.sendEvent("joinRoomResponse", responseObj(false, "Sala no encontrada"));
            return;
        }
        if (room.getPlayers().size() >= 6) {
            client.sendEvent("joinRoomResponse", responseObj(false, "Sala llena"));
            return;
        }
        if (room.isGameStarted()) {
            client.sendEvent("joinRoomResponse", responseObj(false, "Partida en curso"));
            return;
        }

        // Creamos un jugador con ID = client.getSessionId().toString()
        String playerId = client.getSessionId().toString();
        Player newPlayer = new Player(playerId);
        room.addPlayer(newPlayer);

        // Respuesta al cliente
        Map<String,Object> successData = responseObj(true, "Unido a la sala");
        successData.put("playerId", playerId);
        successData.put("roomName", roomName);
        client.sendEvent("roomResponse", successData);

        broadcastPlayers(roomName);
    }

    private void handleStartGame(SocketIOClient client, String roomName, AckRequest ack) {
        Room room = rooms.get(roomName);
        if (room == null) return;

        // Si la sala tiene < 2 jugadores o ya se inició, no hacemos nada
        if (room.getPlayers().size() < 2 || room.isGameStarted()) return;

        room.setGameStarted(true);

        // Generar mazo y jefes
        room.getGameBoard().setDeck(generateDeck());
        room.getGameBoard().setBosses(generateBosses());
        room.getGameBoard().setCurrentBoss(getBoss(room.getGameBoard().getBosses().pop()));

        room.getGameBoard().setPlayerPhase("attack");
        room.getGameBoard().setEndGame(false);
        room.getGameBoard().setWinGame(false);

        // Repartir 5 cartas a cada jugador
        for (Player p : room.getPlayers()) {
            for (int i=0; i<5; i++) {
                p.getHand().add(room.getGameBoard().getDeck().pop());
            }
        }

        // Primer jugador
        Player firstPlayer = room.getPlayers().get(room.getTurnIndex());
        room.getGameBoard().setPlayerTurn(firstPlayer.getId());

        // Enviar la mano a cada jugador
        for (Player p : room.getPlayers()) {
            server.getClient(UUID.fromString(p.getId()))
                    .sendEvent("getPlayerData", Map.of("hand", p.getHand()));
        }

        broadcastBoardStatus(roomName);
    }

    @SuppressWarnings("unchecked")
    private void handlePlayTurn(SocketIOClient client, PlayTurnData playTurnData, AckRequest ack) {
        String roomName = playTurnData.getRoomName();
        String playerId = playTurnData.getPlayerId();
        String action = playTurnData.getAction();
        List<Card> cardsPlayed = playTurnData.getCards();

        System.out.println(">>> playTurn: " + roomName + " " + playerId + " " + action);

        Room room = rooms.get(roomName);
        if (room == null || !room.isGameStarted()) return;

        // Jugador actual según turnIndex
        Player currentPlayer = room.getPlayers().get(room.getTurnIndex());
        if (!currentPlayer.getId().equals(playerId)) {
            // No es su turno
            return;
        }

        int totalPoints = 0;
        boolean multiplePoints = false;

        // Mover cada carta a la mesa y quitarla de la mano del jugador
        for (Card card : cardsPlayed) {
            room.getGameBoard().getTable().add(card);
            currentPlayer.setHand(removeCardFromHand(currentPlayer.getHand(), card));
            totalPoints += getCardPoints(card.getValue());
        }

        // Lógica attack/defend
        if ("attack".equals(action)) {
            room.getGameBoard().setPlayerPhase("defend");

            boolean anyJoker = cardsPlayed.stream().anyMatch(c -> "Joker".equals(c.getSuit()));

            if (anyJoker) {
                // Bloquear efecto del boss
                room.getGameBoard().getCurrentBoss().setEffectBloqued(true);
                nextPlayer(room);
                room.getGameBoard().setPlayerPhase("Joker");
            } else {
                // Revisar palos únicos
                Set<String> suits = new HashSet<>();
                for (Card c : cardsPlayed) {
                    suits.add(c.getSuit());
                }
                for (String suit : suits) {
                    if (!room.getGameBoard().getCurrentBoss().getSuit().equals(suit)
                            || room.getGameBoard().getCurrentBoss().isEffectBloqued()) {

                        switch (suit) {
                            case "♥":
                                // Revivir totalPoints cartas del grave
                                List<Card> revived = room.getGameBoard().popFromGrave(totalPoints);
                                room.getGameBoard().addToDeck(revived);
                                break;
                            case "♦":
                                // Repartir totalPoints cartas
                                int idx = room.getTurnIndex();
                                for (int i = 0; i < totalPoints; i++) {
                                    Player px = room.getPlayers().get(idx);
                                    if (px.getHand().size() < 5 && !room.getGameBoard().getDeck().isEmpty()) {
                                        px.getHand().add(room.getGameBoard().getDeck().pop());
                                    }
                                    idx = (idx + 1) % room.getPlayers().size();
                                }
                                // Actualizar la mano de cada jugador
                                for (Player p : room.getPlayers()) {
                                    sendToPlayer(p.getId(), "getPlayerData", Map.of("hand", p.getHand()));
                                }
                                break;
                            case "♣":
                                multiplePoints = true;
                                break;
                            case "♠":
                                Boss boss = room.getGameBoard().getCurrentBoss();
                                boss.setDamage(boss.getDamage() - totalPoints);
                                break;
                        }
                    }
                }

                // Quitar salud al boss
                Boss boss = room.getGameBoard().getCurrentBoss();
                int effectiveDamage = multiplePoints ? totalPoints * 2 : totalPoints;
                boss.setHealth(boss.getHealth() - effectiveDamage);

                if (boss.getHealth() <= 0) {
                    if (!room.getGameBoard().getBosses().isEmpty()) {
                        room.getGameBoard().setPlayerPhase("attack");
                        if (boss.getHealth() == 0) {
                            // Al principio del deck
                            room.getGameBoard().getDeck().push(new Card(boss.getValue(), boss.getSuit()));
                        } else {
                            // Cementerio
                            room.getGameBoard().getGrave().add(new Card(boss.getValue(), boss.getSuit()));
                        }
                        // Mover cartas de mesa a grave
                        room.getGameBoard().getGrave().addAll(room.getGameBoard().getTable());
                        room.getGameBoard().getTable().clear();

                        // Siguiente boss
                        room.getGameBoard().setCurrentBoss(getBoss(room.getGameBoard().getBosses().pop()));

                    } else {
                        room.getGameBoard().setEndGame(true);
                        room.getGameBoard().setWinGame(true);
                    }
                }
            }

        } else if ("defend".equals(action)) {
            Boss boss = room.getGameBoard().getCurrentBoss();
            if (boss.getDamage() > totalPoints) {
                room.getGameBoard().setEndGame(true);
            } else {
                nextPlayer(room);
                room.getGameBoard().setPlayerPhase("attack");
            }
        }

        // Enviar la mano actualizada al jugador
        sendToPlayer(playerId, "getPlayerData", Map.of("hand", currentPlayer.getHand()));

        // Actualizar el estado del board a todos
        broadcastBoardStatus(roomName);
    }

    /* =========================================================================
       ========================== MÉTODOS AUXILIARES ============================
       ========================================================================= */

    // Siguiente jugador
    private void nextPlayer(Room room) {
        room.setTurnIndex((room.getTurnIndex() + 1) % room.getPlayers().size());
        room.getGameBoard().setPlayerTurn(room.getPlayers().get(room.getTurnIndex()).getId());
    }

    private Map<String,Object> responseObj(boolean success, String msg) {
        Map<String,Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", msg);
        return result;
    }

    private void broadcastRooms() {
        List<String> roomNames = new ArrayList<>(rooms.keySet());
        server.getBroadcastOperations().sendEvent("updateRooms", roomNames);
    }

    private void broadcastPlayers(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) return;
        List<String> playerIds = new ArrayList<>();
        for (Player p : room.getPlayers()) {
            playerIds.add(p.getId());
        }
        server.getBroadcastOperations().sendEvent("updatePlayers", playerIds);
    }

    private void broadcastBoardStatus(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) return;
        server.getBroadcastOperations().sendEvent("boardStatus", room.getGameBoard());
    }

    /**
     * Enviar a un jugador específico (según su ID, que es SessionId.toString())
     */
    private void sendToPlayer(String playerId, String eventName, Object data) {
        UUID uuid = UUID.fromString(playerId);
        SocketIOClient c = server.getClient(uuid);
        if (c != null) {
            c.sendEvent(eventName, data);
        }
    }

    /**
     * Remover una carta de la mano
     */
    private List<Card> removeCardFromHand(List<Card> hand, Card cardToRemove) {
        List<Card> newHand = new ArrayList<>();
        for (Card c : hand) {
            if (c.getValue().equals(cardToRemove.getValue())
                    && c.getSuit().equals(cardToRemove.getSuit())) {
                // se omite
            } else {
                newHand.add(c);
            }
        }
        return newHand;
    }

    private int getCardPoints(String value) {
        switch (value) {
            case "A": return 1;
            case "J": return 10;
            case "Q": return 15;
            case "K": return 20;
            default:
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // Joker u otro
                    return 0;
                }
        }
    }

    // Generar mazo
    private Deque<Card> generateDeck() {
        String[] suits = {"♥", "♦", "♣", "♠"};
        String[] values = {"2","3","4","5","6","7","8","9","10","A"};

        List<Card> tempList = new ArrayList<>();
        for (String s : suits) {
            for (String v : values) {
                tempList.add(new Card(v, s));
            }
        }
        // Jokers
        tempList.add(new Card("0", "Joker"));
        tempList.add(new Card("1", "Joker"));

        Collections.shuffle(tempList);

        return new ArrayDeque<>(tempList);
    }

    // Generar jefes
    private Deque<Card> generateBosses() {
        String[] suits = {"♥","♦","♣","♠"};
        String[] values = {"J","Q","K"};
        List<Card> temp = new ArrayList<>();

        for (String v : values) {
            List<String> shuffledSuits = new ArrayList<>(Arrays.asList(suits));
            Collections.shuffle(shuffledSuits);
            for (String s : shuffledSuits) {
                temp.add(new Card(v, s));
            }
        }
        return new ArrayDeque<>(temp);
    }

    // Crear la info de un Boss
    private Boss getBoss(Card bossCard) {
        Boss boss = new Boss();
        boss.setValue(bossCard.getValue());
        boss.setSuit(bossCard.getSuit());

        switch(boss.getValue()) {
            case "J":
                boss.setHealth(20);
                boss.setDamage(10);
                boss.setEffects("");
                break;
            case "Q":
                boss.setHealth(30);
                boss.setDamage(15);
                break;
            case "K":
                boss.setHealth(40);
                boss.setDamage(20);
                break;
        }

        switch(boss.getSuit()) {
            case "♥":
                boss.setEffects("Bloquea revivir cartas");
                break;
            case "♦":
                boss.setEffects("Bloquea tomar cartas");
                break;
            case "♣":
                boss.setEffects("Bloquea duplicar daño");
                break;
            case "♠":
                boss.setEffects("Bloquea proteger daño");
                break;
        }
        return boss;
    }
}
