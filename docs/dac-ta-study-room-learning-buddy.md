# Đặc tả chức năng Study Room / Learning Buddy

## 1. Tổng quan

### 1.1. Mục tiêu

Phát triển ứng dụng chat hiện tại thành nền tảng học tập cộng tác theo thời gian thực. Chat tiếp tục là nền tảng giao tiếp chính, nhưng mỗi cuộc trò chuyện nhóm được mở rộng thành một **Study Room** có nội dung học tập, câu hỏi, flashcard, quiz, tài liệu và thống kê tiến độ.

Ứng dụng giải quyết ba nhu cầu chính:

- Học và thảo luận cùng người khác theo môn học hoặc mục tiêu cụ thể.
- Chuyển nội dung trò chuyện thành kiến thức có cấu trúc.
- Theo dõi quá trình học thay vì chỉ lưu lịch sử tin nhắn.

Direct message tiếp tục hoạt động như hiện tại. Group chat được mở rộng thành Study Room và tái sử dụng hệ thống thành viên, tin nhắn, upload file, WebSocket và outbox hiện có.

### 1.2. Phạm vi

Các chức năng chính của phiên bản Study Room gồm:

- Study Room theo môn học hoặc chủ đề.
- Question Mode và Accepted Answer.
- AI Explain.
- Flashcards và chế độ ôn tập.
- Mini Quiz realtime.
- Study Timer / Pomodoro Room.
- Resource Library.
- Progress cá nhân.
- Peer Matching.
- Daily Recap.

## 2. Đối tượng sử dụng

- Học sinh, sinh viên học theo nhóm.
- Người tự học ngoại ngữ hoặc lập trình.
- Nhóm thực hiện đồ án.
- Người luyện phỏng vấn.
- Mentor, tutor hoặc người hỗ trợ học tập.

## 3. Study Room

### 3.1. Mô tả

Mỗi Study Room đại diện cho một môn học, kỹ năng hoặc mục tiêu cụ thể, ví dụ: Toán, Java, English, IELTS, đồ án tốt nghiệp hoặc phỏng vấn.

### 3.2. Thông tin phòng

- Tên phòng.
- Ảnh đại diện.
- Mô tả.
- Chủ đề chính.
- Các tag liên quan.
- Trình độ: Beginner, Intermediate hoặc Advanced.
- Ngôn ngữ sử dụng.
- Mục tiêu học tập.
- Chế độ công khai hoặc riêng tư.
- Giới hạn thành viên.
- Lịch học dự kiến.
- Trạng thái: Active hoặc Archived.

### 3.3. Các tab trong phòng

- **Chat:** trao đổi realtime và gửi file như ứng dụng hiện tại.
- **Questions:** quản lý câu hỏi, câu trả lời và Accepted Answer.
- **Resources:** thư viện file, ảnh và liên kết.
- **Flashcards:** bộ thẻ và chế độ ôn tập.
- **Quiz:** tạo, tham gia và xem lịch sử quiz.
- **Progress:** thống kê học tập cá nhân.
- **Members:** danh sách thành viên và quản lý quyền.

### 3.4. Quy tắc nghiệp vụ

- Người tạo phòng trở thành Owner.
- Phòng riêng tư yêu cầu lời mời hoặc phê duyệt tham gia.
- Phòng công khai cho phép người dùng tìm kiếm và gửi yêu cầu tham gia.
- Khi phòng bị lưu trữ, thành viên chỉ được xem dữ liệu cũ và không thể tạo nội dung mới.
- Việc rời phòng không xóa các nội dung người dùng đã đóng góp.

## 4. Thành viên và phân quyền

### 4.1. Vai trò

- **Owner:** quản lý phòng, thành viên, vai trò và toàn bộ nội dung.
- **Moderator:** quản lý câu hỏi, tài liệu, quiz và nội dung vi phạm.
- **Member:** chat, đặt câu hỏi, trả lời, học flashcard và tham gia quiz.

### 4.2. Chính sách quyền

- Tác giả được sửa hoặc xóa câu hỏi và câu trả lời của mình.
- Chủ câu hỏi được chọn hoặc bỏ chọn Accepted Answer.
- Moderator được ẩn nội dung của thành viên khác khi cần thiết.
- Owner được thay đổi vai trò và xóa thành viên khỏi phòng.
- Host của quiz được bắt đầu, điều khiển và kết thúc quiz.
- Chỉ Owner hoặc Moderator được bắt đầu Pomodoro chung nếu phòng không cho phép thành viên tự tạo phiên.

Mô hình phân quyền được áp dụng theo phạm vi từng phòng, kết hợp vai trò và quyền sở hữu tài nguyên. Có thể mô tả là **room-scoped authorization using roles, permissions and ownership policies**.

## 5. Question Mode

### 5.1. Mô tả

Người dùng có thể đánh dấu tin nhắn là `QUESTION` khi gửi hoặc chuyển một tin nhắn đã có thành câu hỏi.

### 5.2. Dữ liệu câu hỏi

- Tiêu đề ngắn.
- Nội dung chi tiết.
- Chủ đề hoặc tag.
- File hoặc ảnh đính kèm.
- Trạng thái: Open, Answered hoặc Resolved.
- Danh sách câu trả lời.
- Accepted Answer.
- Người tạo và thời gian tạo.

### 5.3. Luồng nghiệp vụ

1. Thành viên đăng câu hỏi trong phòng.
2. Hệ thống tạo tin nhắn tương ứng và thông báo cho thành viên liên quan.
3. Thành viên khác trả lời trực tiếp vào câu hỏi.
4. Khi có ít nhất một câu trả lời, trạng thái chuyển từ Open sang Answered.
5. Chủ câu hỏi chọn một câu trả lời làm Accepted Answer.
6. Câu hỏi chuyển sang trạng thái Resolved.
7. Chủ câu hỏi có thể bỏ chọn hoặc chọn một câu trả lời khác.

### 5.4. Tìm kiếm và lọc

- Lọc theo trạng thái.
- Lọc theo tag.
- Lọc theo người tạo.
- Chỉ hiển thị câu hỏi chưa có câu trả lời.
- Tìm kiếm theo tiêu đề và nội dung.

## 6. AI Explain

### 6.1. Đối tượng đầu vào

- Tin nhắn.
- Câu hỏi hoặc câu trả lời.
- Accepted Answer.
- File văn bản hoặc PDF.
- Ảnh có nội dung chữ.
- Nội dung do người dùng nhập trực tiếp.

### 6.2. Các hành động AI

- Giải thích dễ hiểu hơn.
- Giải thích từng bước.
- Tạo ví dụ minh họa.
- Tóm tắt nội dung.
- Dịch Việt sang Anh hoặc Anh sang Việt.
- Tạo câu hỏi luyện tập.
- Tạo flashcard từ nội dung.

### 6.3. Xử lý file

- PDF và file text được trích xuất nội dung trước khi gửi cho AI.
- Ảnh sử dụng OCR hoặc mô hình có khả năng đọc hình ảnh.
- File lớn được chia thành các đoạn để xử lý.
- File không được hỗ trợ phải hiển thị thông báo rõ ràng.

### 6.4. Kết quả

Kết quả AI được hiển thị trong panel riêng và không tự động gửi cho cả phòng. Người dùng có thể:

- Chèn kết quả vào chat.
- Lưu thành ghi chú.
- Tạo flashcard.
- Tạo câu hỏi mới.
- Sao chép nội dung.
- Yêu cầu AI giải thích lại theo cách khác.

Mọi kết quả phải được đánh dấu là nội dung do AI tạo. Hệ thống cần giới hạn kích thước đầu vào, số lần sử dụng và thời gian chờ để kiểm soát chi phí.

## 7. Flashcards

### 7.1. Tạo flashcard

Flashcard có thể được tạo thủ công hoặc tạo từ:

- Tin nhắn.
- Câu hỏi.
- Accepted Answer.
- Tài liệu.
- Kết quả AI Explain.

### 7.2. Dữ liệu flashcard

- Mặt trước.
- Mặt sau.
- Tag.
- Nguồn tạo.
- Người tạo.
- Bộ flashcard.
- Phạm vi cá nhân hoặc dùng chung trong phòng.
- Trạng thái ôn tập riêng của từng thành viên.

### 7.3. Luồng ôn tập

1. Hệ thống hiển thị mặt trước.
2. Người dùng suy nghĩ và chọn lật thẻ.
3. Hệ thống hiển thị mặt sau.
4. Người dùng đánh giá: Again, Hard, Good hoặc Easy.
5. Hệ thống tính thời điểm ôn tiếp theo.
6. Kết quả được cập nhật vào tiến độ cá nhân.

Mỗi thành viên có lịch ôn riêng, kể cả khi dùng chung một bộ flashcard của phòng.

## 8. Mini Quiz Realtime

### 8.1. Tạo quiz

Quiz gồm:

- Tiêu đề.
- Mô tả.
- Danh sách câu hỏi.
- Các lựa chọn.
- Đáp án đúng.
- Giải thích đáp án.
- Thời gian cho từng câu.
- Điểm của từng câu.
- Thiết lập hiển thị bảng xếp hạng.

### 8.2. Luồng realtime

1. Host tạo quiz và mở phòng chờ.
2. Thành viên tham gia phiên quiz.
3. Host bắt đầu quiz.
4. Server phát câu hỏi hiện tại qua WebSocket.
5. Người chơi gửi đáp án trong thời gian cho phép.
6. Server xác nhận và khóa đáp án đã nộp.
7. Khi hết giờ, server phát kết quả và giải thích.
8. Host chuyển sang câu tiếp theo.
9. Khi kết thúc, hệ thống lưu điểm và thống kê.

### 8.3. Quy tắc kỹ thuật

- Server là nguồn thời gian chính.
- Mỗi người chỉ được nộp một đáp án cho mỗi câu, trừ khi quiz cho phép thay đổi trước khi hết giờ.
- Đáp án đúng không được gửi xuống client trước khi câu hỏi kết thúc.
- REST API dùng để tạo quiz và xem lịch sử.
- WebSocket chỉ xử lý trạng thái của phiên quiz đang diễn ra.

## 9. Study Timer / Pomodoro Room

### 9.1. Cấu hình

- Thời gian Focus mặc định: 25 phút.
- Thời gian Break mặc định: 5 phút.
- Số vòng lặp.
- Cho phép tạm dừng hoặc không.
- Chế độ notification trong thời gian Focus.

### 9.2. Trạng thái

- Waiting.
- Focusing.
- Paused.
- Break.
- Completed.

### 9.3. Nguyên tắc đồng bộ

Server lưu `startedAt`, `duration`, `phase` và trạng thái. Client tự tính thời gian còn lại dựa trên dữ liệu này. Server không gửi sự kiện mỗi giây mà chỉ phát sự kiện khi bắt đầu, tạm dừng, tiếp tục, đổi giai đoạn hoặc kết thúc.

### 9.4. Focus Mode

- Giảm hoặc tắt typing indicator.
- Không phát âm thanh cho tin nhắn thông thường.
- Vẫn nhận và lưu tin nhắn realtime.
- Mention trực tiếp có thể tiếp tục hiển thị.
- Khi Focus kết thúc, hiển thị số tin nhắn đã bỏ lỡ.

## 10. Resource Library

### 10.1. Nguồn tài liệu

Tài liệu có thể được thêm trực tiếp vào thư viện hoặc thu thập từ file, ảnh và link đã gửi trong chat.

### 10.2. Dữ liệu tài liệu

- Tên.
- Loại: file, image, video, link hoặc document.
- Tag.
- Mô tả.
- Người tải lên.
- Tin nhắn nguồn.
- Thời gian tạo.
- Trạng thái ghim.
- Số lượt mở hoặc tải xuống.

### 10.3. Chức năng

- Tìm kiếm theo tên và nội dung đã trích xuất.
- Lọc theo loại, tag và người gửi.
- Xem trước tài liệu.
- Ghim tài liệu quan trọng.
- Thêm hoặc sửa tag.
- Mở tin nhắn gốc.
- Xóa tài liệu khỏi thư viện mà không bắt buộc xóa tin nhắn gốc.

## 11. Progress cá nhân

### 11.1. Chỉ số

- Số câu hỏi đã đặt.
- Số câu trả lời đã gửi.
- Số Accepted Answer nhận được.
- Số flashcard đã ôn.
- Tỷ lệ flashcard ghi nhớ.
- Điểm quiz trung bình.
- Tổng thời gian Focus.
- Số ngày học liên tiếp.
- Mức độ hoạt động theo tuần.

### 11.2. Phạm vi hiển thị

- Tổng quan trên toàn ứng dụng.
- Chi tiết theo từng Study Room.
- Theo ngày, tuần hoặc tháng.
- Người dùng được chọn công khai hoặc giữ riêng tiến độ.

Dashboard ưu tiên thể hiện sự tiến bộ cá nhân. Bảng xếp hạng không được bật mặc định để tránh tạo áp lực không cần thiết.

## 12. Peer Matching

### 12.1. Nhu cầu tìm bạn học

Người dùng có thể tạo một yêu cầu, ví dụ: "Tìm bạn học Java từ 20:00 đến 22:00, trình độ Beginner, mục tiêu luyện Spring Boot".

### 12.2. Dữ liệu matching

- Chủ đề.
- Trình độ.
- Mục tiêu.
- Ngôn ngữ.
- Múi giờ.
- Khung giờ rảnh.
- Hình thức học: chat, voice hoặc Pomodoro.
- Số người mong muốn.
- Thời hạn của yêu cầu.

### 12.3. Ghép cặp

Hệ thống tính mức độ phù hợp dựa trên chủ đề, trình độ, lịch rảnh và mục tiêu. Người dùng được xem đề xuất và chủ động gửi lời mời.

Khi hai bên đồng ý, hệ thống có thể:

- Tạo direct conversation.
- Tạo Study Room mới.
- Mời người được ghép vào Study Room có sẵn.

Hệ thống không tự động công khai email, số điện thoại hoặc lịch cá nhân.

## 13. Daily Recap

### 13.1. Nội dung tổng kết

- Các chủ đề đã thảo luận.
- Câu hỏi mới.
- Câu hỏi chưa được trả lời.
- Accepted Answer trong ngày.
- Tài liệu mới.
- Flashcard mới.
- Quiz đã tổ chức và kết quả chung.
- Tổng thời gian Focus.
- Gợi ý nội dung nên ôn tiếp.

### 13.2. Cơ chế tạo recap

1. Job nền thu thập dữ liệu có cấu trúc trong khoảng thời gian cần tổng kết.
2. Hệ thống tạo bản tổng hợp cơ bản từ dữ liệu trong database.
3. AI viết lại bản tổng hợp thành nội dung dễ đọc nếu tính năng AI được bật.
4. Recap được lưu để tránh tạo lại nhiều lần.
5. Thành viên nhận một notification khi recap sẵn sàng.

Recap được hiển thị trong một khu vực riêng của phòng, không gửi thành nhiều tin nhắn gây nhiễu.

## 14. Kiến trúc và giao tiếp

### 14.1. Chức năng sử dụng REST API

- Quản lý Study Room.
- Question và Answer.
- Flashcard và lịch ôn tập.
- Resource Library.
- Tạo quiz và xem lịch sử.
- Progress.
- Peer Matching.
- Daily Recap.
- AI Explain request và lịch sử kết quả.

### 14.2. Chức năng sử dụng WebSocket

- Tin nhắn mới.
- Typing và presence.
- Cập nhật Question và Accepted Answer.
- Quiz đang diễn ra.
- Trạng thái Pomodoro.
- Notification quan trọng trong phòng.

### 14.3. Các realtime event đề xuất

```text
question.created
question.answered
question.accepted
question.reopened
quiz.started
quiz.question.published
quiz.answer.submitted
quiz.question.ended
quiz.completed
timer.started
timer.paused
timer.resumed
timer.phase.changed
timer.completed
resource.created
recap.generated
```

Mỗi event chỉ chứa phần dữ liệu thay đổi. Client gọi REST API khi cần lấy toàn bộ trạng thái. Event phải có `eventId` để client chống xử lý trùng sau khi reconnect.

## 15. Mô hình dữ liệu đề xuất

Các bảng chính cần bổ sung:

- `study_rooms`: thông tin mở rộng của conversation loại Study Room.
- `study_room_tags`: tag của phòng.
- `questions`: câu hỏi và trạng thái xử lý.
- `answers`: câu trả lời và trạng thái Accepted Answer.
- `flashcard_decks`: bộ flashcard.
- `flashcards`: nội dung từng flashcard.
- `flashcard_reviews`: lịch sử và lịch ôn riêng của người dùng.
- `quizzes`: thông tin quiz.
- `quiz_questions`: câu hỏi của quiz.
- `quiz_options`: lựa chọn của câu hỏi.
- `quiz_sessions`: phiên quiz realtime.
- `quiz_submissions`: đáp án và điểm của người tham gia.
- `study_sessions`: phiên Pomodoro.
- `study_session_participants`: người tham gia phiên học.
- `resources`: tài liệu của phòng.
- `resource_tags`: tag của tài liệu.
- `peer_matching_requests`: yêu cầu tìm bạn học.
- `peer_matching_suggestions`: kết quả đề xuất ghép cặp.
- `daily_recaps`: nội dung tổng kết.
- `ai_explanations`: yêu cầu và kết quả AI.
- `learning_progress_daily`: số liệu tiến độ tổng hợp theo ngày.

Các bảng nghiệp vụ nên tham chiếu `conversation_id` hoặc `study_room_id` và giữ nguyên quan hệ với `users`, `messages`, `assets` cùng `conversation_participants` hiện có.

## 16. Yêu cầu phi chức năng

### 16.1. Hiệu năng

- Không gửi toàn bộ trạng thái phòng trong mỗi WebSocket event.
- Timer không phát event mỗi giây.
- Quiz phải chịu được nhiều người trả lời trong cùng thời điểm.
- Resource Library và Questions phải hỗ trợ phân trang.
- Progress và Daily Recap được tổng hợp bằng background job.

### 16.2. Tin cậy

- Realtime event quan trọng được phát sau khi transaction thành công.
- Client đồng bộ lại dữ liệu qua REST sau khi reconnect.
- Quiz submission phải có cơ chế chống gửi trùng.
- AI task cần trạng thái Pending, Processing, Completed và Failed.

### 16.3. Bảo mật

- Kiểm tra quyền theo Study Room cho mọi API và WebSocket action.
- Không tin tưởng role hoặc thời gian quiz do client gửi lên.
- Kiểm tra loại và kích thước file trước khi AI xử lý.
- Không đưa dữ liệu phòng riêng tư vào yêu cầu AI ngoài phạm vi được phép.
- Peer Matching không làm lộ thông tin liên hệ cá nhân.

## 17. Lộ trình triển khai

### Giai đoạn 1 - Study Room MVP

- Study Room profile và các tab cơ bản.
- Question Mode.
- Answer và Accepted Answer.
- Resource Library.
- Progress cơ bản.
- Tái sử dụng chat, upload, group role và WebSocket hiện tại.

### Giai đoạn 2 - Học tập tương tác

- Flashcards.
- Lịch ôn tập cá nhân.
- Mini Quiz realtime.
- Pomodoro Room.
- Focus Mode và notification phù hợp.

### Giai đoạn 3 - AI và kết nối người học

- AI Explain.
- Daily Recap.
- Peer Matching.
- Progress dashboard nâng cao.

## 18. Tiêu chí hoàn thành tổng quát

- Direct message và các chức năng chat hiện tại không bị ảnh hưởng.
- Người dùng có thể tạo Study Room, mời thành viên và học theo chủ đề.
- Câu hỏi có thể nhận nhiều câu trả lời và chỉ có tối đa một Accepted Answer.
- Flashcard chung có trạng thái ôn riêng cho từng người.
- Quiz realtime đồng bộ câu hỏi, thời gian và kết quả giữa các thành viên.
- Pomodoro vẫn đồng bộ đúng sau khi reload hoặc reconnect.
- File và link trong phòng có thể được tìm thấy trong Resource Library.
- Progress được tính từ dữ liệu nghiệp vụ thực tế và không phụ thuộc client.
- Peer Matching chỉ tạo kết nối khi người dùng chủ động đồng ý.
- Daily Recap không bị tạo trùng cho cùng phòng và cùng ngày.
- Các hành động quản trị đều được kiểm tra theo vai trò và quyền sở hữu tài nguyên.
