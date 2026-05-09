# Tài Liệu Nghiệp Vụ Chi Tiết

## 1. Thông tin tài liệu

- Tên tài liệu: Nghiệp vụ cho ứng dụng chat realtime cơ bản
- Mục tiêu: Làm rõ phạm vi, quy trình, luật nghiệp vụ và tiêu chí nghiệm thu cho một ứng dụng chat realtime phiên bản MVP
- Đối tượng sử dụng: Product Owner, Business Analyst, Developer, QA, UI/UX Designer
- Trạng thái: Draft baseline

## 2. Mục tiêu sản phẩm

Ứng dụng chat realtime cơ bản cho phép người dùng:

- Đăng ký và đăng nhập tài khoản
- Tìm kiếm người dùng khác trong hệ thống
- Bắt đầu cuộc trò chuyện 1-1
- Gửi và nhận tin nhắn văn bản theo thời gian thực
- Theo dõi trạng thái online, đang nhập và đã đọc cơ bản
- Quản lý danh sách hội thoại và số tin nhắn chưa đọc

Mục tiêu của bản MVP là cung cấp trải nghiệm nhắn tin đơn giản, ổn định, dễ mở rộng cho các tính năng nâng cao sau này như nhóm chat, gửi file, gọi thoại hoặc push notification đa nền tảng.

## 3. Phạm vi triển khai

### 3.1. Trong phạm vi MVP

- Quản lý tài khoản cơ bản
- Hồ sơ người dùng tối thiểu
- Tìm kiếm người dùng
- Tạo và truy cập hội thoại 1-1
- Gửi và nhận tin nhắn text realtime
- Hiển thị trạng thái tin nhắn: gửi, đã nhận, đã đọc
- Hiển thị trạng thái hiện diện cơ bản: online/offline
- Hiển thị chỉ báo đang nhập
- Danh sách hội thoại gần nhất
- Đếm số tin nhắn chưa đọc

### 3.2. Ngoài phạm vi MVP

- Nhóm chat nhiều người
- Gửi ảnh, file, video, voice message
- Thu hồi hoặc chỉnh sửa tin nhắn
- Xóa hội thoại hai chiều
- Mã hóa đầu cuối
- Gọi audio/video
- Story, timeline, social feed
- Tích hợp AI assistant
- Push notification mobile production-grade
- Chức năng quản trị nâng cao như báo cáo vi phạm, khóa tài khoản hàng loạt

## 4. Bối cảnh nghiệp vụ

Sản phẩm phục vụ nhu cầu trao đổi tức thời giữa hai người dùng đã có tài khoản trong cùng một hệ thống. Mô hình ưu tiên:

- Trao đổi cá nhân nhanh
- Thấy tin nhắn mới gần như ngay lập tức
- Không cần tải lại trang để cập nhật hội thoại
- Giảm thao tác khởi tạo cuộc trò chuyện

Ứng dụng phù hợp với:

- Chat nội bộ đơn giản
- Ứng dụng cộng đồng nhỏ
- Nền tảng học tập hoặc dịch vụ cần nhắn tin trực tiếp

## 5. Vai trò người dùng

### 5.1. Người dùng cuối

Có thể:

- Tạo tài khoản
- Đăng nhập
- Cập nhật hồ sơ cơ bản
- Tìm kiếm người dùng khác
- Bắt đầu trò chuyện
- Gửi và đọc tin nhắn

### 5.2. Hệ thống

Chịu trách nhiệm:

- Xác thực người dùng
- Đồng bộ trạng thái kết nối realtime
- Lưu trữ hội thoại và tin nhắn
- Phân phối tin nhắn đến đúng người nhận
- Tính toán số chưa đọc
- Đồng bộ trạng thái đã đọc

### 5.3. Quản trị viên

Trong MVP, vai trò quản trị viên chỉ ở mức vận hành hệ thống, không bắt buộc có giao diện quản trị riêng. Nếu cần, quản trị viên có thể:

- Xem log lỗi
- Theo dõi trạng thái dịch vụ
- Can thiệp ở mức dữ liệu khi có sự cố

## 6. Thuật ngữ

- User: Người dùng có tài khoản trong hệ thống
- Realtime: Dữ liệu được đẩy gần như ngay lập tức giữa các client đang kết nối
- Conversation: Hội thoại giữa hai người dùng
- Participant: Thành viên thuộc hội thoại
- Message: Tin nhắn văn bản do người dùng gửi
- Presence: Trạng thái hiện diện như online hoặc offline
- Unread Count: Số lượng tin nhắn chưa đọc trong một hội thoại
- Read Receipt: Dấu hiệu cho biết tin nhắn đã được người nhận đọc
- Typing Indicator: Chỉ báo người còn lại đang nhập tin nhắn

## 7. Giả định nghiệp vụ

- Mỗi tài khoản là duy nhất theo email hoặc username
- Một cặp người dùng chỉ có tối đa một hội thoại 1-1 hoạt động tại cùng thời điểm
- Tin nhắn được lưu lịch sử để người dùng xem lại khi đăng nhập lại
- Tin nhắn được sắp xếp theo thời điểm tạo
- Một người dùng có thể đăng nhập trên nhiều thiết bị, nhưng ở MVP có thể đơn giản hóa thành một phiên hoạt động chính
- Hệ thống ưu tiên gửi text message, không yêu cầu xử lý media ở giai đoạn đầu

## 8. Luồng nghiệp vụ tổng quan

### 8.1. Đăng ký và vào hệ thống

1. Người dùng mở ứng dụng.
2. Người dùng chọn đăng ký hoặc đăng nhập.
3. Hệ thống xác thực thông tin.
4. Sau khi đăng nhập thành công, người dùng được chuyển vào màn hình danh sách hội thoại.
5. Hệ thống đồng bộ trạng thái kết nối và đánh dấu người dùng online nếu đang kết nối realtime.

### 8.2. Bắt đầu cuộc trò chuyện

1. Người dùng tìm kiếm người khác theo tên hiển thị, username hoặc email.
2. Người dùng chọn một kết quả.
3. Hệ thống kiểm tra đã tồn tại hội thoại 1-1 hay chưa.
4. Nếu đã tồn tại, mở lại hội thoại cũ.
5. Nếu chưa tồn tại, tạo hội thoại mới và thêm vào danh sách hội thoại của cả hai bên.

### 8.3. Gửi và nhận tin nhắn

1. Người gửi nhập nội dung.
2. Người gửi bấm gửi.
3. Hệ thống kiểm tra tính hợp lệ của nội dung.
4. Tin nhắn được lưu thành công vào cơ sở dữ liệu.
5. Hệ thống đẩy tin nhắn realtime tới người nhận nếu người nhận đang online.
6. Danh sách hội thoại của cả hai bên được cập nhật tin nhắn cuối và thời gian cập nhật.
7. Nếu người nhận chưa mở hội thoại, hệ thống tăng số tin chưa đọc.

### 8.4. Đọc tin nhắn

1. Người nhận mở hội thoại.
2. Client gửi sự kiện đã xem hội thoại.
3. Hệ thống cập nhật các tin nhắn phù hợp sang trạng thái đã đọc.
4. Hệ thống phát sự kiện trạng thái đã đọc cho phía người gửi.
5. Số lượng chưa đọc của hội thoại được đưa về 0 trên phía người nhận.

## 9. Chức năng nghiệp vụ chi tiết

## 9.1. Quản lý tài khoản

### BR-ACC-01. Đăng ký tài khoản

Mục tiêu:

- Cho phép người dùng mới tạo tài khoản để sử dụng ứng dụng

Thông tin đầu vào tối thiểu:

- Tên hiển thị
- Username hoặc email đăng nhập
- Mật khẩu
- Xác nhận mật khẩu

Luồng chính:

1. Người dùng nhập thông tin đăng ký.
2. Hệ thống kiểm tra dữ liệu bắt buộc.
3. Hệ thống kiểm tra username hoặc email chưa tồn tại.
4. Hệ thống kiểm tra mật khẩu thỏa điều kiện tối thiểu.
5. Hệ thống tạo tài khoản mới.
6. Hệ thống trả về kết quả thành công và cho phép chuyển sang đăng nhập hoặc tự động đăng nhập.

Luật nghiệp vụ:

- Username hoặc email phải là duy nhất.
- Mật khẩu phải có độ dài tối thiểu, ví dụ từ 8 ký tự.
- Tên hiển thị không được để trống.
- Nếu xác nhận mật khẩu không khớp, không được tạo tài khoản.

Ngoại lệ:

- Username hoặc email đã tồn tại
- Dữ liệu nhập thiếu hoặc sai định dạng
- Lỗi hệ thống khi lưu tài khoản

Tiêu chí nghiệm thu:

- Không tạo trùng tài khoản
- Thông báo lỗi rõ ràng theo từng trường
- Sau khi tạo thành công, tài khoản có thể đăng nhập ngay

### BR-ACC-02. Đăng nhập

Mục tiêu:

- Cho phép người dùng truy cập hệ thống an toàn

Đầu vào:

- Username hoặc email
- Mật khẩu

Luồng chính:

1. Người dùng nhập thông tin đăng nhập.
2. Hệ thống xác thực tài khoản và mật khẩu.
3. Nếu hợp lệ, hệ thống phát hành phiên đăng nhập hoặc token.
4. Client dùng thông tin xác thực này để gọi API và kết nối kênh realtime.

Luật nghiệp vụ:

- Tài khoản chỉ đăng nhập được khi thông tin xác thực đúng.
- Nếu sai mật khẩu nhiều lần, có thể log lại phục vụ vận hành. Khóa tài khoản tạm thời là tùy chọn, chưa bắt buộc trong MVP.

Tiêu chí nghiệm thu:

- Đăng nhập thành công thì vào được màn hình chính
- Sai thông tin thì không tạo phiên
- Client kết nối realtime được sau khi có phiên hợp lệ

### BR-ACC-03. Đăng xuất

Mục tiêu:

- Cho phép người dùng kết thúc phiên sử dụng

Luồng chính:

1. Người dùng chọn đăng xuất.
2. Client xóa phiên cục bộ.
3. Hệ thống hoặc client đóng kết nối realtime.
4. Người dùng bị điều hướng về màn hình đăng nhập.

Luật nghiệp vụ:

- Sau đăng xuất, các yêu cầu cần xác thực phải bị từ chối nếu dùng lại phiên cũ.

### BR-ACC-04. Cập nhật hồ sơ cơ bản

Phạm vi thông tin có thể cập nhật:

- Tên hiển thị
- Ảnh đại diện
- Trạng thái mô tả ngắn

Luật nghiệp vụ:

- Người dùng chỉ được cập nhật hồ sơ của chính mình.
- Dữ liệu mới phải được phản ánh ở danh sách hội thoại và header chat trong các lần tải tiếp theo.

## 9.2. Tìm kiếm người dùng

### BR-USER-01. Tìm kiếm người dùng

Mục tiêu:

- Hỗ trợ người dùng tìm đối tượng để bắt đầu trò chuyện

Tiêu chí tìm kiếm:

- Tên hiển thị
- Username
- Email nếu nghiệp vụ cho phép hiển thị

Luồng chính:

1. Người dùng nhập từ khóa.
2. Hệ thống trả về danh sách kết quả phù hợp.
3. Người dùng chọn một tài khoản để xem nhanh hoặc bắt đầu chat.

Luật nghiệp vụ:

- Không hiển thị chính bản thân người dùng trong kết quả bắt đầu chat.
- Chỉ hiển thị tài khoản hợp lệ, đang hoạt động.
- Có thể giới hạn số kết quả mỗi lần trả về để tối ưu hiệu năng.

Tiêu chí nghiệm thu:

- Kết quả trả về đúng theo từ khóa
- Không cho phép tạo hội thoại với chính mình

## 9.3. Quản lý hội thoại 1-1

### BR-CONV-01. Tạo hoặc mở hội thoại

Mục tiêu:

- Bảo đảm một cặp người dùng có thể chat mà không sinh trùng hội thoại

Luồng chính:

1. Người dùng chọn "nhắn tin" với một user khác.
2. Hệ thống kiểm tra hội thoại 1-1 giữa hai bên đã tồn tại hay chưa.
3. Nếu có, trả về hội thoại hiện hữu.
4. Nếu chưa có, tạo mới hội thoại và thêm hai participant.

Luật nghiệp vụ:

- Mỗi cặp người dùng chỉ có một hội thoại 1-1 đang hoạt động.
- Hội thoại mới chỉ xuất hiện trong danh sách khi được tạo thành công.
- Hội thoại không có tin nhắn vẫn có thể tồn tại nếu người dùng đã chủ động mở chat.

Tiêu chí nghiệm thu:

- Không tạo trùng hội thoại khi hai bên cùng mở chat gần như đồng thời
- Mở lại đúng lịch sử nếu hội thoại đã tồn tại

### BR-CONV-02. Danh sách hội thoại

Mục tiêu:

- Cho phép người dùng theo dõi các cuộc trò chuyện gần nhất

Thông tin hiển thị tối thiểu:

- Tên và avatar đối phương
- Nội dung tin nhắn cuối cùng
- Thời điểm cập nhật cuối
- Số tin nhắn chưa đọc
- Trạng thái online hoặc offline nếu có

Luật nghiệp vụ:

- Danh sách hội thoại sắp xếp giảm dần theo thời điểm cập nhật cuối.
- Khi có tin nhắn mới, hội thoại được đẩy lên đầu danh sách.
- Nếu hội thoại không có tin nhắn, có thể hiển thị dòng mô tả như "Chưa có tin nhắn".

Tiêu chí nghiệm thu:

- Thứ tự danh sách cập nhật đúng khi gửi hoặc nhận tin nhắn mới
- Số chưa đọc hiển thị chính xác theo từng hội thoại

## 9.4. Nhắn tin realtime

### BR-MSG-01. Gửi tin nhắn văn bản

Mục tiêu:

- Cho phép gửi nội dung text trong hội thoại 1-1

Đầu vào:

- Conversation ID
- Nội dung tin nhắn
- Thời điểm gửi từ client, nếu cần để hiển thị tạm thời

Luồng chính:

1. Người dùng nhập nội dung.
2. Client gửi yêu cầu gửi tin nhắn.
3. Hệ thống xác thực người gửi thuộc hội thoại đó.
4. Hệ thống kiểm tra nội dung hợp lệ.
5. Hệ thống lưu tin nhắn.
6. Hệ thống phát tin nhắn mới tới các client liên quan.
7. Giao diện cập nhật ngay tin nhắn trong luồng chat.

Luật nghiệp vụ:

- Không cho gửi tin nhắn rỗng sau khi trim khoảng trắng.
- Giới hạn độ dài một tin nhắn, ví dụ 2000 ký tự.
- Người gửi chỉ được gửi vào hội thoại mà mình là participant.
- Mỗi tin nhắn phải có mã định danh duy nhất và thời điểm tạo.
- Tin nhắn phải lưu bền vững trước khi xác nhận gửi thành công.

Trạng thái tối thiểu của tin nhắn:

- Sending: client đang chờ phản hồi
- Sent: hệ thống đã lưu thành công
- Delivered: người nhận đã nhận được trên ít nhất một phiên hoạt động
- Read: người nhận đã mở hội thoại và đọc

Tiêu chí nghiệm thu:

- Tin nhắn hợp lệ được lưu và phản ánh trên giao diện gần như ngay lập tức
- Tin nhắn lỗi không được hiển thị là thành công
- Người không thuộc hội thoại không gửi được tin vào hội thoại đó

### BR-MSG-02. Nhận tin nhắn realtime

Mục tiêu:

- Người nhận thấy tin nhắn mới mà không cần tải lại trang

Luồng chính:

1. Khi có tin nhắn mới, hệ thống xác định các phiên đang kết nối của người nhận.
2. Hệ thống đẩy dữ liệu tin nhắn qua kênh realtime.
3. Client nhận sự kiện và cập nhật giao diện.
4. Nếu hội thoại đang mở, tin nhắn hiển thị trực tiếp vào khung chat.
5. Nếu hội thoại chưa mở, tăng badge chưa đọc và đẩy hội thoại lên đầu danh sách.

Luật nghiệp vụ:

- Nếu người nhận offline, tin nhắn vẫn phải được lưu để đọc lại sau.
- Khi người nhận online lại, lịch sử tin nhắn phải tải được đầy đủ theo cơ chế phân trang hoặc tải gần nhất.

### BR-MSG-03. Tải lịch sử tin nhắn

Mục tiêu:

- Người dùng xem lại nội dung hội thoại cũ

Luồng chính:

1. Người dùng mở một hội thoại.
2. Hệ thống trả về tập tin nhắn gần nhất.
3. Khi người dùng cuộn lên trên, hệ thống tải thêm dữ liệu cũ hơn nếu còn.

Luật nghiệp vụ:

- Lịch sử tin nhắn phải sắp xếp đúng theo thời gian.
- Cần hỗ trợ phân trang hoặc cursor để tránh tải toàn bộ lịch sử một lần.
- Người dùng không được tải lịch sử của hội thoại mà mình không tham gia.

Tiêu chí nghiệm thu:

- Mở hội thoại hiển thị đúng các tin gần nhất
- Cuộn tải thêm không lặp hoặc mất tin

### BR-MSG-04. Đồng bộ trạng thái đã đọc

Mục tiêu:

- Phản ánh việc người nhận đã xem tin nhắn

Luồng chính:

1. Người nhận mở hội thoại và màn hình đang ở trạng thái có thể xem tin.
2. Client gửi sự kiện cập nhật đã đọc đến mốc tin nhắn hiện tại.
3. Hệ thống cập nhật trạng thái read cho các tin phù hợp.
4. Hệ thống đẩy thông tin read receipt sang phía người gửi.

Luật nghiệp vụ:

- Chỉ các tin do người khác gửi mới được đánh dấu đã đọc.
- Không cho phép lùi trạng thái từ read về delivered hoặc sent.
- Trạng thái đã đọc thường áp dụng liên tục đến mốc tin mới nhất đã nhìn thấy.

Tiêu chí nghiệm thu:

- Khi người nhận mở chat, badge chưa đọc giảm đúng về 0 cho hội thoại đó
- Người gửi thấy dấu đã đọc đúng theo mốc

## 9.5. Trạng thái hiện diện và hành vi realtime

### BR-RT-01. Online hoặc offline

Mục tiêu:

- Cho biết đối phương đang có kết nối hoạt động hay không

Luật nghiệp vụ:

- Người dùng được xem là online khi có ít nhất một kết nối realtime hợp lệ đang mở.
- Khi mọi kết nối đều đóng hoặc timeout, trạng thái chuyển về offline.
- Có thể lưu thêm thời điểm hoạt động cuối để hiển thị "last seen" trong tương lai, nhưng chưa bắt buộc hiển thị ở MVP.

Tiêu chí nghiệm thu:

- Trạng thái online phản ánh thay đổi trong thời gian ngắn sau khi đăng nhập hoặc rời ứng dụng

### BR-RT-02. Chỉ báo đang nhập

Mục tiêu:

- Cải thiện cảm giác realtime trong hội thoại

Luồng chính:

1. Người dùng bắt đầu nhập trong ô chat.
2. Client phát tín hiệu typing tới hệ thống.
3. Hệ thống chuyển tiếp tín hiệu cho người còn lại trong cùng hội thoại.
4. Khi người dùng dừng nhập hoặc gửi tin, trạng thái typing được xóa.

Luật nghiệp vụ:

- Typing indicator chỉ là dữ liệu tạm thời, không cần lưu dài hạn.
- Không phát typing nếu người dùng không thuộc hội thoại.
- Tín hiệu typing nên có timeout ngắn để tự tắt nếu mất kết nối hoặc client không gửi stop typing.

Tiêu chí nghiệm thu:

- Người nhận thấy chỉ báo đang nhập khi đối phương đang soạn tin
- Chỉ báo tự mất khi hết timeout hoặc khi có tin nhắn được gửi

## 10. Luật nghiệp vụ tổng hợp

### 10.1. Luật về tài khoản

- Mỗi email hoặc username chỉ thuộc về một tài khoản.
- Người dùng chưa đăng nhập không được truy cập dữ liệu hội thoại cá nhân.
- Người dùng chỉ sửa được dữ liệu hồ sơ của chính mình.

### 10.2. Luật về hội thoại

- Mỗi cặp user chỉ có một hội thoại 1-1 duy nhất trong trạng thái hoạt động.
- Chỉ participant mới xem được danh sách tin nhắn của hội thoại.
- Hội thoại được cập nhật `lastMessageAt` mỗi khi có tin mới.

### 10.3. Luật về tin nhắn

- Tin nhắn phải có nội dung hợp lệ và không rỗng.
- Tin nhắn phải được gắn người gửi, hội thoại và thời điểm tạo.
- Tin nhắn không được mất khi người nhận offline.
- Thứ tự hiển thị mặc định là tăng dần theo thời gian trong khung chat.

### 10.4. Luật về trạng thái

- `Sent` chỉ xác nhận hệ thống đã lưu.
- `Delivered` chỉ xác nhận client người nhận đã nhận sự kiện hoặc đã đồng bộ thành công.
- `Read` chỉ xác nhận người nhận đã xem hội thoại.
- Trạng thái không được đi lùi.

## 11. Dữ liệu cốt lõi cần quản lý

### 11.1. User

Thông tin tối thiểu:

- ID
- Username
- Email
- Password hash
- Display name
- Avatar URL
- Bio hoặc status message
- Created at
- Updated at
- Account status

### 11.2. Conversation

Thông tin tối thiểu:

- ID
- Type, trong MVP là `DIRECT`
- Created at
- Updated at
- Last message ID
- Last message at

### 11.3. ConversationParticipant

Thông tin tối thiểu:

- Conversation ID
- User ID
- Joined at
- Last read message ID hoặc last read timestamp
- Unread count có thể tính động hoặc lưu sẵn

### 11.4. Message

Thông tin tối thiểu:

- ID
- Conversation ID
- Sender ID
- Content
- Message type, trong MVP là `TEXT`
- Created at
- Updated at nếu cần
- Delivery status hoặc read metadata

### 11.5. Presence

Thông tin có thể quản lý:

- User ID
- Is online
- Last active at
- Connection count hoặc session references

## 12. Màn hình nghiệp vụ đề xuất

### 12.1. Màn hình đăng ký

Chức năng:

- Nhập thông tin đăng ký
- Hiển thị lỗi nhập liệu
- Chuyển sang đăng nhập

### 12.2. Màn hình đăng nhập

Chức năng:

- Nhập tài khoản và mật khẩu
- Hiển thị thông báo sai thông tin
- Điều hướng vào ứng dụng khi thành công

### 12.3. Màn hình danh sách hội thoại

Chức năng:

- Hiển thị profile hiện tại
- Thanh tìm kiếm người dùng hoặc tìm kiếm hội thoại
- Danh sách hội thoại gần nhất
- Badge số chưa đọc
- Trạng thái online của đối phương nếu có

### 12.4. Màn hình chi tiết hội thoại

Chức năng:

- Header thông tin đối phương
- Vùng hiển thị lịch sử tin nhắn
- Ô nhập nội dung
- Nút gửi
- Hiển thị trạng thái đang nhập và đã đọc

### 12.5. Màn hình hồ sơ cá nhân

Chức năng:

- Xem và sửa tên hiển thị
- Cập nhật avatar
- Cập nhật status ngắn

## 13. Quy tắc hiển thị giao diện

- Tin nhắn của bản thân căn phải, tin nhắn của đối phương căn trái
- Mốc thời gian hiển thị theo định dạng thân thiện với người dùng
- Khi đang gửi, tin nhắn có trạng thái chờ
- Khi lỗi gửi, giao diện cần báo lỗi và cho phép gửi lại nếu sản phẩm muốn hỗ trợ
- Badge chưa đọc chỉ hiển thị khi số lượng lớn hơn 0
- Chỉ báo đang nhập chỉ hiển thị tại hội thoại đang mở

## 14. Yêu cầu phi chức năng

### 14.1. Hiệu năng

- Tin nhắn sau khi gửi thành công nên hiển thị ở cả hai phía trong khoảng thời gian ngắn, mục tiêu dưới 1 đến 2 giây trong điều kiện mạng ổn định
- Danh sách hội thoại và lịch sử tin nhắn phải tải được trong thời gian chấp nhận được với dữ liệu cỡ MVP

### 14.2. Tính sẵn sàng

- Nếu kết nối realtime bị gián đoạn, client cần có cơ chế kết nối lại
- Sau khi kết nối lại, dữ liệu hội thoại phải được đồng bộ lại mà không mất tin

### 14.3. Bảo mật

- Mật khẩu phải lưu dưới dạng băm
- Các API dữ liệu người dùng và hội thoại phải yêu cầu xác thực
- Người dùng không được truy cập hội thoại không thuộc về mình
- Kênh realtime phải gắn với danh tính đã xác thực

### 14.4. Khả năng mở rộng

- Thiết kế dữ liệu nên sẵn sàng mở rộng sang group chat
- Hệ thống nên tách rõ nghiệp vụ gửi tin, trạng thái tin và presence

### 14.5. Nhật ký và giám sát

- Cần log các sự kiện lỗi khi xác thực, gửi tin hoặc kết nối realtime thất bại
- Nên có metric cơ bản như số kết nối đồng thời, số tin nhắn gửi mỗi phút, lỗi gửi tin

## 15. Quy tắc xử lý lỗi và tình huống biên

### 15.1. Người dùng offline

- Nếu người nhận offline, tin nhắn vẫn lưu thành công
- Khi người nhận vào lại hệ thống, tin nhắn chưa đọc phải hiển thị đầy đủ

### 15.2. Gửi trùng do mạng chập chờn

- Nên có cơ chế idempotency từ client hoặc message client ID để tránh sinh trùng khi người dùng bấm gửi nhiều lần

### 15.3. Mất kết nối realtime trong lúc chat

- Client cần thông báo trạng thái kết nối
- Khi kết nối lại, cần tải lại phần dữ liệu thiếu kể từ mốc cuối đã đồng bộ

### 15.4. Hai người cùng tạo hội thoại đồng thời

- Backend phải có ràng buộc tránh tạo hai hội thoại 1-1 trùng nhau

### 15.5. Tin nhắn quá dài hoặc rỗng

- Hệ thống từ chối và trả lỗi rõ ràng

## 16. Báo cáo và chỉ số vận hành cơ bản

Trong MVP, chưa cần dashboard nghiệp vụ phức tạp, nhưng nên theo dõi:

- Số người dùng đăng ký mới
- Số người dùng hoạt động hàng ngày
- Số hội thoại được tạo
- Số tin nhắn gửi thành công
- Tỷ lệ lỗi gửi tin
- Thời gian phản hồi trung bình của gửi tin

## 17. Phân rã backlog mức epic và user story

### Epic 1. Tài khoản và xác thực

- Là người dùng mới, tôi muốn đăng ký tài khoản để bắt đầu sử dụng ứng dụng.
- Là người dùng, tôi muốn đăng nhập để truy cập lịch sử chat của mình.
- Là người dùng, tôi muốn đăng xuất để bảo vệ tài khoản trên thiết bị dùng chung.

### Epic 2. Hồ sơ người dùng

- Là người dùng, tôi muốn cập nhật tên hiển thị và avatar để người khác nhận diện tôi dễ hơn.

### Epic 3. Tìm kiếm và khởi tạo chat

- Là người dùng, tôi muốn tìm kiếm người khác để bắt đầu trò chuyện.
- Là người dùng, tôi muốn mở lại hội thoại cũ thay vì tạo hội thoại trùng.

### Epic 4. Nhắn tin realtime

- Là người dùng, tôi muốn gửi tin nhắn văn bản để trao đổi ngay lập tức.
- Là người dùng, tôi muốn nhận tin nhắn mới mà không cần tải lại trang.
- Là người dùng, tôi muốn xem lịch sử tin nhắn cũ khi mở hội thoại.

### Epic 5. Trạng thái hội thoại

- Là người dùng, tôi muốn thấy số tin chưa đọc để ưu tiên phản hồi.
- Là người dùng, tôi muốn biết đối phương đã đọc tin hay chưa.
- Là người dùng, tôi muốn thấy khi đối phương đang nhập để cuộc trò chuyện tự nhiên hơn.

## 18. Tiêu chí nghiệm thu mức hệ thống

- Người dùng đăng ký, đăng nhập và đăng xuất thành công theo đúng luồng
- Người dùng tìm được user khác và bắt đầu chat 1-1
- Không sinh trùng hội thoại 1-1 giữa cùng một cặp user
- Tin nhắn text được gửi, lưu và hiển thị realtime
- Khi người nhận offline, tin nhắn vẫn tồn tại và đọc lại được sau
- Danh sách hội thoại cập nhật tin cuối, thời gian và số chưa đọc chính xác
- Trạng thái online, typing và read hoạt động đúng trong mức MVP
- Người dùng không thể truy cập dữ liệu hội thoại của người khác

## 19. Đề xuất thứ tự triển khai

### Giai đoạn 1

- Đăng ký
- Đăng nhập
- Hồ sơ cơ bản

### Giai đoạn 2

- Tìm kiếm user
- Tạo hội thoại 1-1
- Danh sách hội thoại

### Giai đoạn 3

- Gửi và nhận tin nhắn realtime
- Lịch sử tin nhắn
- Số chưa đọc

### Giai đoạn 4

- Online hoặc offline
- Typing indicator
- Read receipt

## 20. Rủi ro nghiệp vụ cần lưu ý

- Không ràng buộc unique conversation sẽ gây trùng hội thoại 1-1
- Không tách rõ `sent`, `delivered`, `read` sẽ làm sai kỳ vọng người dùng
- Không xử lý reconnect và đồng bộ bù sẽ gây mất trải nghiệm realtime
- Không kiểm soát quyền participant sẽ gây lộ dữ liệu hội thoại
- Không quy định message length từ đầu sẽ phát sinh vấn đề hiệu năng và UI

## 21. Kết luận

Tài liệu này mô tả baseline nghiệp vụ cho một ứng dụng chat realtime cơ bản theo hướng MVP. Trọng tâm là xác thực, hội thoại 1-1, gửi nhận tin nhắn text realtime, trạng thái đọc và hiện diện cơ bản. Đây là nền tảng đủ rõ để:

- Chuyển thành backlog triển khai
- Viết API contract
- Thiết kế giao diện
- Viết test case nghiệp vụ
- Mở rộng dần sang group chat, media message và notification trong các giai đoạn sau
