    private void sendProfileMenu(String chatId, UUID targetUuid) {
        Player target = Bukkit.getPlayer(targetUuid);
        boolean isOnline = (target != null && target.isOnline());
        String ip = isOnline ? target.getAddress().getAddress().getHostAddress() : "Неизвестен (Оффлайн)";
        String status = isOnline ? "🟢 Онлайн" : "🔴 Оффлайн";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👤 Профиль игрока:\n🆔 UUID: " + targetUuid + "\n🌐 IP: " + ip + "\n📊 Статус: " + status);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton lockBtn = new InlineKeyboardButton();
        lockBtn.setText("🔒 Заблокировать");
        lockBtn.setCallbackData("lock_" + targetUuid);
        
        InlineKeyboardButton unlockBtn = new InlineKeyboardButton();
        unlockBtn.setText("🔓 Разблокировать");
        // ИСПРАВЛЕНО: Ошибочный метод setKeyboard(null) удален
        unlockBtn.setCallbackData("unlock_" + targetUuid);
        
        row1.add(lockBtn);
        row1.add(unlockBtn);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton kickBtn = new InlineKeyboardButton();
        kickBtn.setText("🥾 Кикнуть");
        kickBtn.setCallbackData("kick_" + targetUuid);
        row2.add(kickBtn);

        rowsInline.add(row1);
        rowsInline.add(row2);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
