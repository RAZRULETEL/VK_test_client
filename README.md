# VK Android client
Клиент под Android для соцсети VK, написанный с использованием следующих библиотек:
+ [Timber](https://github.com/JakeWharton/timber) - logging
+ [Room](https://developer.android.com/reference/androidx/room/package-summary) - local DB
+ [Volley](https://google.github.io/volley/) - network requests

**На данынй момент реализовано:**
+ просмотр списка диалогов, а также сообщений в них
+ отображение следующих типов вложений: стикеры, фотографии, видео, пересланные сообщения
+ полное кэширование сообщений, их истории изменений и вложений
+ уведомления на входящие сообщения, которые для каждого диалога объединяются в отдельную группу
