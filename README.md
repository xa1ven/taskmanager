# 📋 Планировщик задач

> Курсовая работа по дисциплине «Разработка клиент-серверных мобильных приложений»

---

## 🛠 Стек технологий

### Android-клиент
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)

### Сервер
![Ktor](https://img.shields.io/badge/Ktor-087CFA?style=for-the-badge&logo=kotlin&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

---

## 📁 Структура репозитория

```
taskmanager/
├── app/                  # Android-приложение (Kotlin + Jetpack Compose)
│   └── src/main/kotlin/com/example/todolist/
│       ├── data/         # Модели, API, репозитории
│       ├── di/           # Hilt-модули
│       └── ui/           # Экраны, ViewModel, тема
└── ktor-server/          # Серверная часть (Ktor + Exposed)
    └── src/main/kotlin/com/taskmanager/
        ├── models/       # Таблицы БД и data-классы
        ├── plugins/      # JWT, БД, роутинг, сериализация
        └── routes/       # Обработчики эндпоинтов
```

---

## ✨ Функциональность

- 🔐 **Авторизация** — регистрация и вход с JWT-токеном
- ✅ **Задачи** — создание, редактирование, удаление, архивирование
- 🔗 **Связанные задачи** — возможность связывать задачи между собой
- 🔍 **Поиск** с историей последних 10 запросов
- 👆 **Свайп влево** для архивирования задачи
- 🌙 **Тёмная тема** и настройка внешнего вида
- 🔔 **Уведомления** — утром и вечером о задачах на день
- 📦 **Архив** выполненных задач

---

## 🏗 Архитектура

```
Android (MVVM)  ──HTTP/REST──▶  Ktor Server  ──Exposed ORM──▶  PostgreSQL
                   + JWT
```

**Клиент:** `MVVM` + `Hilt` + `Retrofit` + `Coroutines` + `DataStore`

**Сервер:** `Ktor` + `Exposed` + `HikariCP` + `BCrypt` + `Docker Compose`

---

## 🗄 База данных

| Таблица | Назначение |
|---|---|
| `users` | Пользователи (логин + хэш пароля) |
| `tasks` | Задачи (название, описание, приоритет, дедлайн, статус) |
| `task_relations` | Симметричные связи между задачами |

---

## 🚀 Запуск сервера

### Требования
- Docker + Docker Compose

### Команды

```bash
git clone https://github.com/xa1ven/taskmanager.git
cd taskmanager/ktor-server

# Сборка JAR
gradle shadowJar --no-daemon

# Сборка Docker-образа
docker build -t ktor-app:latest .

# Запуск всего стека (PostgreSQL + Ktor + Adminer)
cd ..
docker compose up -d
```

Сервер будет доступен на `http://localhost:8000`

### Переменные окружения

| Переменная | Описание |
|---|---|
| `DB_URL` | JDBC-строка подключения к PostgreSQL |
| `DB_USER` | Пользователь БД |
| `DB_PASSWORD` | Пароль БД |
| `JWT_SECRET` | Секрет для подписи JWT-токенов |

---

## 📡 API

| Метод | Эндпоинт | Описание |
|---|---|---|
| `POST` | `/auth/register` | Регистрация |
| `POST` | `/auth/login` | Вход |
| `PATCH` | `/auth/password` | Смена пароля |
| `GET` | `/tasks?query=` | Список задач (с поиском) |
| `POST` | `/tasks` | Создать задачу |
| `PUT` | `/tasks/{id}` | Обновить задачу |
| `DELETE` | `/tasks/{id}` | Удалить задачу |
| `PATCH` | `/tasks/{id}/done` | Архивировать задачу |
| `POST` | `/tasks/{id}/relations` | Добавить связь |
| `DELETE` | `/tasks/{id}/relations/{relId}` | Удалить связь |

> Все эндпоинты кроме `/auth/register` и `/auth/login` требуют заголовок `Authorization: Bearer <token>`

---

## 📱 Экраны приложения

| Экран | Описание |
|---|---|
| Вход / Регистрация | Авторизация пользователя |
| Главная | Список активных задач + поиск |
| Архив | Выполненные задачи |
| Детали задачи | Просмотр, связанные задачи |
| Создание / Редактирование | Форма задачи |
| Настройки | Тема, шрифт, цвета, пароль |
| О приложении | Версия и информация |

---

## 👤 Автор

**xa1ven** — [github.com/xa1ven](https://github.com/xa1ven)

---

*Приложение создано в образовательных целях в рамках выполнения курсовой работы.*
