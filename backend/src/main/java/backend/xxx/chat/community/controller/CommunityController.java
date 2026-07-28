package backend.xxx.chat.community.controller;

import java.util.List;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.community.dto.CommunityCategoryResponse;
import backend.xxx.chat.community.dto.CommunityChannelResponse;
import backend.xxx.chat.community.dto.CommunityDetailResponse;
import backend.xxx.chat.community.dto.CommunitySummaryResponse;
import backend.xxx.chat.community.dto.CommunityTagResponse;
import backend.xxx.chat.community.dto.CreateCommunityChannelRequest;
import backend.xxx.chat.community.dto.CreateCommunityRequest;
import backend.xxx.chat.community.dto.UpdateCommunityChannelRequest;
import backend.xxx.chat.community.dto.UpdateCommunityRequest;
import backend.xxx.chat.community.service.CommunityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Validated
public class CommunityController {

    private final CurrentUserProvider currentUserProvider;
    private final CommunityService communityService;

    @GetMapping("/categories")
    public ResponseData<List<CommunityCategoryResponse>> getCategories() {
        return new ResponseData<>(true, "community.category.list.success", communityService.getCategories());
    }

    @GetMapping("/tags")
    public ResponseData<List<CommunityTagResponse>> getTags() {
        return new ResponseData<>(true, "community.tag.list.success", communityService.getTags());
    }

    @GetMapping("/communities")
    public ResponseData<List<CommunitySummaryResponse>> discoverCommunities(
            @Min(1) @Max(50) @RequestParam(name = "limit", required = false, defaultValue = "20") Short limit,
            @RequestParam(name = "category", required = false) String categorySlug,
            @RequestParam(name = "tag", required = false) String tagSlug,
            @RequestParam(name = "q", required = false) String search
    ) {
        return new ResponseData<>(true, "community.list.success", communityService.discoverCommunities(
                currentUserProvider.getCurrentUsername(),
                limit,
                categorySlug,
                tagSlug,
                search
        ));
    }

    @PostMapping("/communities")
    public ResponseEntity<ResponseData<CommunityDetailResponse>> createCommunity(
            @Valid @RequestBody CreateCommunityRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>(
                true,
                "community.create.success",
                communityService.createCommunity(currentUserProvider.getCurrentUsername(), request)
        ));
    }

    @GetMapping("/communities/{slug}")
    public ResponseData<CommunityDetailResponse> getCommunityDetail(@PathVariable String slug) {
        return new ResponseData<>(true, "community.detail.success", communityService.getCommunityDetail(
                currentUserProvider.getCurrentUsername(),
                slug
        ));
    }

    @PatchMapping("/communities/{communityId}")
    public ResponseData<CommunityDetailResponse> updateCommunity(
            @Positive @PathVariable Long communityId,
            @Valid @RequestBody UpdateCommunityRequest request
    ) {
        return new ResponseData<>(true, "community.update.success", communityService.updateCommunity(
                currentUserProvider.getCurrentUsername(),
                communityId,
                request
        ));
    }

    @PostMapping("/communities/{communityId}/join")
    public ResponseData<CommunityDetailResponse> joinCommunity(@Positive @PathVariable Long communityId) {
        return new ResponseData<>(true, "community.join.success", communityService.joinCommunity(
                currentUserProvider.getCurrentUsername(),
                communityId
        ));
    }

    @PostMapping("/communities/{communityId}/leave")
    public ResponseData<Void> leaveCommunity(@Positive @PathVariable Long communityId) {
        communityService.leaveCommunity(currentUserProvider.getCurrentUsername(), communityId);
        return new ResponseData<>(true, "community.leave.success");
    }

    @PostMapping("/communities/{communityId}/channels")
    public ResponseEntity<ResponseData<CommunityChannelResponse>> createChannel(
            @Positive @PathVariable Long communityId,
            @Valid @RequestBody CreateCommunityChannelRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>(
                true,
                "community.channel.create.success",
                communityService.createChannel(currentUserProvider.getCurrentUsername(), communityId, request)
        ));
    }

    @PatchMapping("/channels/{channelId}")
    public ResponseData<CommunityChannelResponse> updateChannel(
            @Positive @PathVariable Long channelId,
            @Valid @RequestBody UpdateCommunityChannelRequest request
    ) {
        return new ResponseData<>(true, "community.channel.update.success", communityService.updateChannel(
                currentUserProvider.getCurrentUsername(),
                channelId,
                request
        ));
    }
}
