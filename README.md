# TicTacToe Netty Game

## Описание
Это проект представляет собой многопользовательскую игру "Крестики-нолики", реализованную с использованием библиотеки Netty для работы с сетевыми соединениями в Java. 
Игроки могут подключаться к серверу через клиент и играть в игру в режиме реального времени. Сервер обрабатывает игровые ходы, сообщает состояние доски и определяет победителя.

## Основные возможности
- Поддержка двух игроков.
- Ведение игры с синхронизацией ходов между двумя клиентами.
- Определение победителя или ничьей.
- Оповещение игроков о результатах и автоматическое отключение после завершения игры.

## Структура проекта
Проект состоит из следующих основных классов:
- **TicTacToeServer** — серверная часть, управляющая всей игровой логикой, а также поддерживающая сетевые соединения.
- **TicTacToeClient** — клиентская часть, через которую игроки могут подключаться к серверу и делать свои ходы.

## Запуск проекта
Для успешного запуска проекта вам нужно запустить сервер и подключить два клиента.

### Требования
- Java 17 или выше.
- Maven для сборки проекта.


